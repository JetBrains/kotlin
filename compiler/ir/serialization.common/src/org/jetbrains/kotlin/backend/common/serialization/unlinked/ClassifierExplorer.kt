/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExploredClassifier.Unusable
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExploredClassifier.Unusable.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExploredClassifier.Usable
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.DeclarationId
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.DeclarationId.Companion.declarationId
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.isEffectivelyMissingLazyIrDeclaration
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.addIfNotNull

internal class ClassifierExplorer(private val builtIns: IrBuiltIns, private val stubGenerator: MissingDeclarationStubGenerator) {
    private val exploredSymbols = ExploredClassifiers()

    private val permittedAnnotationArrayParameterSymbols: Set<IrClassSymbol> by lazy {
        setOf(
            builtIns.stringClass, // kotlin.String
            builtIns.kClassClass // kotlin.reflect.KClass
        )
    }

    private val permittedAnnotationParameterSymbols: Set<IrClassSymbol> by lazy {
        buildSet {
            this += permittedAnnotationArrayParameterSymbols

            PrimitiveType.values().forEach {
                addIfNotNull(builtIns.findClass(it.typeName, BUILT_INS_PACKAGE_FQ_NAME)) // kotlin.<primitive>
                addIfNotNull(builtIns.findClass(it.arrayTypeName, BUILT_INS_PACKAGE_FQ_NAME)) // kotlin.<primitive>Array
            }

            UnsignedType.values().forEach {
                addIfNotNull(builtIns.findClass(it.typeName, BUILT_INS_PACKAGE_FQ_NAME)) // kotlin.U<signed>
                addIfNotNull(builtIns.findClass(it.arrayClassId.shortClassName, BUILT_INS_PACKAGE_FQ_NAME)) // kotlin.U<signed>Array
            }
        }
    }

    fun exploreType(type: IrType): Unusable? = type.exploreType(visitedSymbols = hashSetOf()).asUnusable()
    fun exploreSymbol(symbol: IrClassifierSymbol): Unusable? = symbol.exploreSymbol(visitedSymbols = hashSetOf()).asUnusable()

    fun exploreIrElement(element: IrElement) {
        element.acceptChildrenVoid(IrElementExplorer { it.exploreType(visitedSymbols = hashSetOf()) })
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
            is IrClass -> {
                if (classifier.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                    return exploredSymbols.registerUnusable(this, MissingClassifier(this))

                // TODO: if the class is from stdlib, don't run all the checks below

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
        return valueParameters.firstUnusable { valueParameter ->
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
            // Can inherit only from other interfaces.
            val realSuperClassSymbols = superTypeSymbols.filter { it != builtIns.anyClass && !it.owner.isInterface }
            if (realSuperClassSymbols.isNotEmpty())
                return InvalidInheritance(symbol, realSuperClassSymbols) // Interface inherits from a real class(es).
        } else {
            // Check the number of non-interface supertypes.
            val superClassSymbols = superTypeSymbols.filter { !it.owner.isInterface }
            val superClassSymbol = when (superClassSymbols.size) {
                0 -> builtIns.anyClass
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

            // Check delegating constructor calls.
            val ownConstructors: Map<IrConstructorSymbol, IrConstructor> = declarations
                .filterIsInstance<IrConstructor>()
                .associateBy { it.symbol }

            ownConstructors.forEach { (_, constructor) ->
                var unexpectedSuperClassConstructorSymbol: IrConstructorSymbol? = null

                constructor.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        if (unexpectedSuperClassConstructorSymbol != null)
                            return // Break.

                        element.acceptChildrenVoid(this)
                    }

                    override fun visitClass(declaration: IrClass) {
                        // Skip nested
                    }

                    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
                        if (unexpectedSuperClassConstructorSymbol != null)
                            return // Break.

                        val calledConstructorSymbol = expression.symbol
                        if (calledConstructorSymbol in ownConstructors) return // OK, just calling another constructor of the same class

                        if (calledConstructorSymbol.isBound) {
                            val calledConstructor = calledConstructorSymbol.owner
                            if (calledConstructor.origin != PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION) {
                                val constructedSuperClassSymbol = calledConstructor.parentAsClass.symbol
                                if (constructedSuperClassSymbol != superClassSymbol) {
                                    // Wrong delegating constructor call.
                                    unexpectedSuperClassConstructorSymbol = calledConstructorSymbol
                                }
                            }
                        } else {
                            // Fallback to signatures.
                            (calledConstructorSymbol.signature as? IdSignature.CommonSignature)?.let { constructorSignature ->
                                val constructedSuperClassId = DeclarationId(
                                    constructorSignature.packageFqName,
                                    constructorSignature.declarationFqName.substringBeforeLast('.')
                                )

                                if (superClassSymbol.owner.declarationId != constructedSuperClassId) {
                                    // Wrong delegating constructor call.
                                    unexpectedSuperClassConstructorSymbol = calledConstructorSymbol
                                }
                            }
                        }
                    }
                })

                if (unexpectedSuperClassConstructorSymbol != null)
                    return InvalidInheritance(symbol, superClassSymbol, unexpectedSuperClassConstructorSymbol!!)
            }
        }

        return null
    }

    companion object {
        private inline val IrClass.annotationConstructorsIfApplicable: Sequence<IrConstructor>?
            get() = if (isAnnotationClass) constructors else null

        private inline val IrClass.outerClassSymbolIfApplicable: IrClassSymbol?
            get() = if (isInner || isEnumEntry) (parent as? IrClass)?.symbol else null

        private inline val IrTypeArgument.typeOrNull: IrType?
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

private class IrElementExplorer(private val visitType: (IrType) -> Unit) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        visitType(declaration.type)
        super.visitValueParameter(declaration)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        declaration.superTypes.forEach(visitType)
        super.visitTypeParameter(declaration)
    }

    override fun visitFunction(declaration: IrFunction) {
        visitType(declaration.returnType)
        super.visitFunction(declaration)
    }

    override fun visitField(declaration: IrField) {
        visitType(declaration.type)
        super.visitField(declaration)
    }

    override fun visitVariable(declaration: IrVariable) {
        visitType(declaration.type)
        super.visitVariable(declaration)
    }

    override fun visitExpression(expression: IrExpression) {
        visitType(expression.type)
        super.visitExpression(expression)
    }

    override fun visitClassReference(expression: IrClassReference) {
        visitType(expression.classType)
        super.visitClassReference(expression)
    }

    override fun visitConstantObject(expression: IrConstantObject) {
        expression.typeArguments.forEach(visitType)
        super.visitConstantObject(expression)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        visitType(expression.typeOperand)
        super.visitTypeOperator(expression)
    }
}
