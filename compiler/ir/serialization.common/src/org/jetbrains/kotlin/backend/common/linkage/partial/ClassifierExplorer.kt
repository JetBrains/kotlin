/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageUtils.isEffectivelyMissingLazyIrDeclaration
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.linkage.partial.ExploredClassifier
import org.jetbrains.kotlin.ir.linkage.partial.ExploredClassifier.Unusable
import org.jetbrains.kotlin.ir.linkage.partial.ExploredClassifier.Unusable.*
import org.jetbrains.kotlin.ir.linkage.partial.ExploredClassifier.Usable
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.psi2ir.lazy.IrLazyClass
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageUtils.Module as PLModule

internal class ClassifierExplorer(
    private val builtIns: IrBuiltIns,
    private val stubGenerator: MissingDeclarationStubGenerator,
) {
    private val exploredSymbols = ExploredClassifiers()

    private val permittedAnnotationArrayParameterSymbols: Set<IrClassSymbol> by lazy {
        setOf(
            builtIns.stringClass, // kotlin.String
            builtIns.kClassClass // kotlin.reflect.KClass
        )
    }

    @OptIn(InternalSymbolFinderAPI::class)
    private val permittedAnnotationParameterSymbols: Set<IrClassSymbol> by lazy {
        permittedAnnotationArrayParameterSymbols + setOf(
            builtIns.booleanClass, builtIns.booleanArray,
            builtIns.charClass, builtIns.charArray,
            builtIns.floatClass, builtIns.floatArray,
            builtIns.doubleClass, builtIns.doubleArray,

            builtIns.byteClass, builtIns.byteArray,
            builtIns.shortClass, builtIns.shortArray,
            builtIns.intClass, builtIns.intArray,
            builtIns.longClass, builtIns.longArray,

            builtIns.ubyteClass, builtIns.ubyteArray,
            builtIns.ushortClass, builtIns.ushortArray,
            builtIns.uintClass, builtIns.uintArray,
            builtIns.ulongClass, builtIns.ulongArray,
        ).filterNotNull()
    }

    private val stdlibModule by lazy { PLModule.determineModuleFor(builtIns.anyClass.owner) }

    fun exploreType(type: IrType): Unusable? = type.exploreType(visitedSymbols = hashSetOf()).asUnusable()
    fun exploreSymbol(symbol: IrClassifierSymbol): Unusable? = symbol.exploreSymbol(visitedSymbols = hashSetOf()).asUnusable()

    fun exploreIrElement(element: IrElement) {
        element.acceptChildrenVoid(
            object : IrTypeVisitorVoid() {
                override fun visitType(container: IrElement, type: IrType) {}
                override fun visitTypeRecursively(container: IrElement, type: IrType) {
                    type.exploreType(visitedSymbols = hashSetOf())
                }
            }
        )
    }

    /** Explore the IR type to find the first cause why this type should be considered as unusable. */
    private fun IrType.exploreType(visitedSymbols: MutableSet<IrClassifierSymbol>): ExploredClassifier {
        return when (this) {
            is IrSimpleType -> classifier.exploreSymbol(visitedSymbols).asUnusable()
                ?: arguments.firstUnusable { it.typeOrNull?.exploreType(visitedSymbols) }
                ?: Usable
            is IrDynamicType -> Usable
            else -> throw IllegalArgumentException("Unsupported IR type: ${this::class.java}, $this")
        }
    }

    /** Explore the IR classifier symbol to find the first cause why this symbol should be considered as unusable. */
    private fun IrClassifierSymbol.exploreSymbol(visitedSymbols: MutableSet<IrClassifierSymbol>): ExploredClassifier {
        exploredSymbols[this]?.let { result ->
            // Already explored and registered symbol.
            return result
        }

        if (!isBound) {
            stubGenerator.getDeclaration(this) // Generate a stub and bind the symbol immediately.
            return exploredSymbols.registerUnusable(this, MissingClassifier(this))
        }

        (owner as? IrLazyClass)?.let { lazyIrClass ->
            val isEffectivelyMissingClassifier =
                /* Lazy IR declaration is present but wraps a special "not found" class descriptor. */
                lazyIrClass.descriptor is NotFoundClasses.MockClassDescriptor
                        /* The outermost class containing the lazy IR declaration is private, which normally should not happen
                         * because the declaration is exported from the module. */
                        || lazyIrClass.isEffectivelyMissingLazyIrDeclaration()

            if (isEffectivelyMissingClassifier)
                return exploredSymbols.registerUnusable(this, MissingClassifier(this))
        }

        if (!visitedSymbols.add(this)) {
            return Usable // Recursion avoidance.
        }

        val cause: Unusable? = when (val classifier = owner) {
            is IrClass -> when (PLModule.determineModuleFor(owner as IrClass)) {
                is PLModule.MissingDeclarations -> return exploredSymbols.registerUnusable(this, MissingClassifier(this))
                stdlibModule, PLModule.SyntheticBuiltInFunctions -> {
                    // Don't run any additional checks if the class is from stdlib.
                    null
                }
                else -> {
                    // Class from non-stdlib module.
                    val directSuperTypeSymbols = hashSetOf<IrClassSymbol>()

                    classifier.annotationConstructorsIfApplicable?.firstUnusable { it.exploreAnnotationConstructor(visitedSymbols) }
                        ?: classifier.outerClassSymbolIfApplicable?.exploreSymbol(visitedSymbols).asUnusable()
                        ?: classifier.typeParameters.firstUnusable { it.symbol.exploreSymbol(visitedSymbols) }
                        ?: classifier.superTypes.firstUnusable { superType ->
                            directSuperTypeSymbols.addIfNotNull(superType.asSimpleType()?.classifier as? IrClassSymbol)
                            superType.exploreType(visitedSymbols)
                        }
                        ?: classifier.exploreSuperClasses(directSuperTypeSymbols) // Check them all at once.
                }
            }

            is IrTypeParameter -> classifier.superTypes.firstUnusable { it.exploreType(visitedSymbols) }

            else -> null
        }

        val rootCause = when {
            cause == null -> return exploredSymbols.registerUsable(this)
            cause.symbol == this -> return exploredSymbols.registerUnusable(this, cause)
            else -> when (cause) {
                is DueToOtherClassifier -> cause.rootCause
                is CanBeRootCause -> cause
            }
        }

        return exploredSymbols.registerUnusable(this, DueToOtherClassifier(this, rootCause))
    }

    private fun IrConstructor.exploreAnnotationConstructor(visitedSymbols: MutableSet<IrClassifierSymbol>): Unusable? {
        return parameters.firstUnusable { valueParameter ->
            valueParameter.type.exploreType(visitedSymbols).asUnusable()
                ?: valueParameter.exploreAnnotationConstructorParameter(visitedSymbols, annotationClass = parentAsClass)
        }
    }

    /** See also [org.jetbrains.kotlin.resolve.CompileTimeConstantUtils.isAcceptableTypeForAnnotationParameter] */
    private fun IrValueParameter.exploreAnnotationConstructorParameter(
        visitedSymbols: MutableSet<IrClassifierSymbol>,
        annotationClass: IrClass
    ): Unusable? {
        val parameterType = type.asSimpleType() ?: return null
        val parameterClassSymbol = parameterType.classifier as IrClassSymbol
        val parameterClass = parameterClassSymbol.owner

        when {
            parameterClass.isAnnotationClass -> {
                // Recurse on another annotation.
                parameterClassSymbol.exploreSymbol(visitedSymbols).asUnusable()?.let { return it }
            }

            parameterClass.isEnumClass || parameterClassSymbol in permittedAnnotationParameterSymbols -> return null // Permitted.

            parameterClassSymbol == builtIns.arrayClass -> {
                // Additional checks for array element type.
                for (argument in parameterType.arguments) {
                    val argumentClassSymbol = (argument.typeOrNull?.asSimpleType() ?: continue).classifier as IrClassSymbol
                    val argumentClass = argumentClassSymbol.owner

                    when {
                        argumentClass.isAnnotationClass -> {
                            // Recurse on another annotation.
                            argumentClassSymbol.exploreSymbol(visitedSymbols).asUnusable()?.let { return it }
                        }

                        argumentClass.isEnumClass || argumentClassSymbol in permittedAnnotationArrayParameterSymbols -> continue // Permitted.

                        else -> return AnnotationWithUnacceptableParameter(annotationClass.symbol, argumentClassSymbol)
                    }
                }
            }

            else -> return AnnotationWithUnacceptableParameter(annotationClass.symbol, parameterClassSymbol)
        }

        return null
    }

    private fun IrClass.exploreSuperClasses(superTypeSymbols: Set<IrClassSymbol>): Unusable? {
        if (isInterface) {
            // Regular interface can inherit only from other regular interfaces.
            // External interface can inherit from external interfaces and external class, but not regular ones.
            val illegalSuperClassSymbols = if (isExternal)
                superTypeSymbols.filter { superTypeSymbol ->
                    superTypeSymbol != builtIns.anyClass && superTypeSymbol.owner.let { superClass ->
                        !superClass.isExternal || !(superClass.isInterface || superClass.isClass)
                    }
                }
            else
                superTypeSymbols.filter { it != builtIns.anyClass && !it.owner.isInterface }

            if (illegalSuperClassSymbols.isNotEmpty())
                return InvalidInheritance(symbol, illegalSuperClassSymbols)
        } else {
            // Check the number of non-interface supertypes.
            val superClassSymbols = superTypeSymbols.filter { !it.owner.isInterface }
            val superClassSymbol = when (superClassSymbols.size) {
                0 -> return null // It can be only Any.
                1 -> superClassSymbols.first()
                else -> return InvalidInheritance(symbol, superClassSymbols) // Class inherits from multiple classes.
            }

            // Super class can not be final or of illegal kind.
            if (superClassSymbol != builtIns.anyClass
                && superClassSymbol != builtIns.enumClass // Enum class can't be explicitly inherited, only valid enum class can inherit it.
            ) {
                val superClass = superClassSymbol.owner

                // Note: Super-interfaces are already filtered out above.
                val isInvalidInheritance = when (kind) {
                    ClassKind.INTERFACE,
                    ClassKind.ENUM_CLASS,
                    ClassKind.ANNOTATION_CLASS -> true
                    else -> isValue || when (superClass.kind) {
                        ClassKind.ENUM_CLASS -> kind != ClassKind.ENUM_ENTRY
                        ClassKind.CLASS -> superClass.modality == Modality.FINAL
                        else -> true
                    }
                }

                if (isInvalidInheritance)
                    return InvalidInheritance(symbol, superClassSymbols) // Invalid inheritance.
            }
        }

        return null
    }

    companion object {
        private val IrClass.annotationConstructorsIfApplicable: Sequence<IrConstructor>?
            get() = if (isAnnotationClass) constructors else null

        // Note: Don't consider any nested class automatically as unlinked if enclosing class is unlinked.
        private val IrClass.outerClassSymbolIfApplicable: IrClassSymbol?
            get() = if (isInner || isEnumEntry) (parent as? IrClass)?.symbol else null

        private val IrTypeArgument.typeOrNull: IrType?
            get() = (this as? IrTypeProjection)?.type

        private fun IrType.asSimpleType() = this as? IrSimpleType

        private fun ExploredClassifier?.asUnusable() = this as? Unusable

        /** Iterate the collection and find the first unusable classifier. */
        private inline fun <T> Iterable<T>.firstUnusable(transform: (T) -> ExploredClassifier?): Unusable? =
            firstNotNullOfOrNull { transform(it).asUnusable() }

        private inline fun <T> Sequence<T>.firstUnusable(transform: (T) -> ExploredClassifier?): Unusable? =
            firstNotNullOfOrNull { transform(it).asUnusable() }
    }
}
