/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private const val PUBLIC_MANGLE_FLAG = 1L shl 63

abstract class KotlinManglerImpl : KotlinMangler {

    override val String.hashMangle get() = (this.cityHash64() % PUBLIC_MANGLE_FLAG) or PUBLIC_MANGLE_FLAG

    private fun hashedMangleImpl(declaration: IrDeclaration): Long {
        return declaration.uniqSymbolName().hashMangle
    }

    override val IrDeclaration.hashedMangle: Long
        get() = hashedMangleImpl(this)


    // We can't call "with (super) { this.isExported() }" in children.
    // So provide a hook.
    protected open fun IrDeclaration.isPlatformSpecificExported(): Boolean = false

    override fun IrDeclaration.isExported(): Boolean = isExportedImpl(this)

    /**
     * Defines whether the declaration is exported, i.e. visible from other modules.
     *
     * Exported declarations must have predictable and stable ABI
     * that doesn't depend on any internal transformations (e.g. IR lowering),
     * and so should be computable from the descriptor itself without checking a backend state.
     */
    private tailrec fun isExportedImpl(declaration: IrDeclaration): Boolean {
        // TODO: revise
        val descriptorAnnotations = declaration.descriptor.annotations

        if (declaration.isPlatformSpecificExported()) return true

        if (declaration is IrTypeAlias && declaration.parent is IrPackageFragment) {
            return true
        }

        if (descriptorAnnotations.hasAnnotation(publishedApiAnnotation)) {
            return true
        }

        if (declaration.isAnonymousObject)
            return false

        if (declaration is IrConstructor && declaration.constructedClass.kind.isSingleton) {
            // Currently code generator can access the constructor of the singleton,
            // so ignore visibility of the constructor itself.
            return isExportedImpl(declaration.constructedClass)
        }

        if (declaration is IrFunction) {
            val descriptor = declaration.descriptor
            // TODO: this code is required because accessor doesn't have a reference to property.
            if (descriptor is PropertyAccessorDescriptor) {
                val property = descriptor.correspondingProperty
                if (property.annotations.hasAnnotation(publishedApiAnnotation)) return true
            }
        }

        val visibility = when (declaration) {
            is IrClass -> declaration.visibility
            is IrFunction -> declaration.visibility
            is IrProperty -> declaration.visibility
            is IrField -> declaration.visibility
            is IrTypeAlias -> declaration.visibility
            else -> null
        }

        /**
         * note: about INTERNAL - with support of friend modules we let frontend to deal with internal declarations.
         */
        if (visibility != null && !visibility.isPublicAPI && visibility != Visibilities.INTERNAL) {
            // If the declaration is explicitly marked as non-public,
            // then it must not be accessible from other modules.
            return false
        }

        val parent = declaration.parent
        if (parent is IrDeclaration) {
            return isExportedImpl(parent)
        }

        return true
    }

    private val publishedApiAnnotation = FqName("kotlin.PublishedApi")

    protected fun acyclicTypeMangler(visited: MutableSet<IrTypeParameter>, type: IrType): String {
        val descriptor = (type.classifierOrNull as? IrTypeParameterSymbol)?.owner
        if (descriptor != null) {
            val upperBounds = if (visited.contains(descriptor)) "" else {

                visited.add(descriptor)

                descriptor.superTypes.map {
                    val bound = acyclicTypeMangler(visited, it)
                    if (bound == "kotlin.Any?") "" else "_$bound"
                }.joinToString("")
            }
            return "#GENERIC${if (type.isMarkedNullable()) "?" else ""}$upperBounds"
        }

        var hashString = type.getClass()?.run { fqNameForIrSerialization.asString() } ?: "<dynamic>"

        when (type) {
            is IrSimpleType -> {
                if (!type.arguments.isEmpty()) {
                    hashString += "<${type.arguments.map {
                        when (it) {
                            is IrStarProjection -> "#STAR"
                            is IrTypeProjection -> {
                                val variance = it.variance.label
                                val projection = if (variance == "") "" else "${variance}_"
                                projection + acyclicTypeMangler(visited, it.type)
                            }
                            else -> error(it)
                        }
                    }.joinToString(",")}>"
                }

                if (type.hasQuestionMark) hashString += "?"
            }
            !is IrDynamicType -> {
                error(type)
            }
        }
        return hashString
    }

    protected fun typeToHashString(type: IrType) = acyclicTypeMangler(mutableSetOf(), type)

    val IrValueParameter.extensionReceiverNamePart: String
        get() = "@${typeToHashString(this.type)}."

    open val IrFunction.argsPart
        get() = this.valueParameters.map {
            "${typeToHashString(it.type)}${if (it.isVararg) "_VarArg" else ""}"
        }.joinToString(";")

    open val IrFunction.signature: String
        get() {
            val extensionReceiverPart = this.extensionReceiverParameter?.extensionReceiverNamePart ?: ""
            val argsPart = this.argsPart
            // Distinguish value types and references - it's needed for calling virtual methods through bridges.
            // Also is function has type arguments - frontend allows exactly matching overrides.
            val signatureSuffix =
                when {
                    this.typeParameters.isNotEmpty() -> "Generic"
                    returnType.isInlined -> "ValueType"
                    !returnType.isUnitOrNullableUnit() -> typeToHashString(returnType)
                    else -> ""
                }
            return "$extensionReceiverPart($argsPart)$signatureSuffix"
        }

    open val IrFunction.platformSpecificFunctionName: String? get() = null

    // TODO: rename to indicate that it has signature included
    override val IrFunction.functionName: String
        get() {
            // TODO: Again. We can't call super in children, so provide a hook for now.
            this.platformSpecificFunctionName?.let { return it }
            val name = this.name.mangleIfInternal(this.module, this.visibility)
            return "$name$signature"
        }

    override val Long.isSpecial: Boolean
        get() = specialHashes.contains(this)

    fun Name.mangleIfInternal(moduleDescriptor: ModuleDescriptor, visibility: Visibility): String =
        if (visibility != Visibilities.INTERNAL) {
            this.asString()
        } else {
            val moduleName = moduleDescriptor.name.asString()
                .let { it.substring(1, it.lastIndex) } // Remove < and >.

            "$this\$$moduleName"
        }

    val IrField.symbolName: String
        get() {
            val containingDeclarationPart = parent.fqNameForIrSerialization.let {
                if (it.isRoot) "" else "$it."
            }
            return "kfield:$containingDeclarationPart$name"

        }

    val IrClass.typeInfoSymbolName: String
        get() {
            assert(isExportedImpl(this))
            if (isBuiltInFunction(this))
                return KotlinMangler.functionClassSymbolName(name)
            return "ktype:" + this.fqNameForIrSerialization.toString()
        }

    val IrTypeParameter.symbolName: String
        get() {

            val parentDeclaration = (parent as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: parent
            val containingDeclarationPart = when (parentDeclaration) {
                is IrDeclaration -> parentDeclaration.uniqSymbolName()
                else -> error("Unexpected type parameter parent")
            }
            return "ktypeparam:$containingDeclarationPart$name@$index"
        }

    val IrTypeAlias.symbolName: String
        get() {
            val containingDeclarationPart = parent.fqNameForIrSerialization.let {
                if (it.isRoot) "" else "$it."
            }
            return "ktypealias:$containingDeclarationPart$name"
        }

// This is a little extension over what's used in real mangling
// since some declarations never appear in the bitcode symbols.

    internal fun IrDeclaration.uniqSymbolName(): String = when (this) {
        is IrFunction -> this.uniqFunctionName
        is IrProperty -> this.symbolName
        is IrClass -> this.typeInfoSymbolName
        is IrField -> this.symbolName
        is IrEnumEntry -> this.symbolName
        is IrTypeParameter -> this.symbolName
        is IrTypeAlias -> this.symbolName
        else -> error("Unexpected exported declaration: $this")
    }

    private val IrDeclarationParent.fqNameUnique: FqName
        get() = when (this) {
            is IrPackageFragment -> this.fqName
            is IrDeclaration -> this.parent.fqNameUnique.child(this.uniqName)
            else -> error(this)
        }

    private val IrDeclaration.uniqName: Name
        get() = when (this) {
            is IrSimpleFunction -> Name.special("<${this.uniqFunctionName}>")
            else -> this.nameForIrSerialization
        }

    private val IrProperty.symbolName: String
        get() {
            val extensionReceiver: String = getter?.extensionReceiverParameter?.extensionReceiverNamePart ?: ""

            val containingDeclarationPart = parent.fqNameForIrSerialization.let {
                if (it.isRoot) "" else "$it."
            }
            return "kprop:$containingDeclarationPart$extensionReceiver$name"
        }

    private val IrEnumEntry.symbolName: String
        get() {
            val containingDeclarationPart = parent.fqNameForIrSerialization.let {
                if (it.isRoot) "" else "$it."
            }
            return "kenumentry:$containingDeclarationPart$name"
        }

    // This is basicly the same as .symbolName, but disambiguates external functions with the same C name.
// In addition functions appearing in fq sequence appear as <full signature>.
    private val IrFunction.uniqFunctionName: String
        get() {
            if (isBuiltInFunction(this))
                return KotlinMangler.functionInvokeSymbolName(parentAsClass.name)
            val parent = this.parent

            val containingDeclarationPart = parent.fqNameUnique.let {
                if (it.isRoot) "" else "$it."
            }

            return "kfun:$containingDeclarationPart#$functionName"
        }

    private val specialHashes = listOf("Function", "KFunction", "SuspendFunction", "KSuspendFunction")
        .flatMap { name ->
            (0..255).map { KotlinMangler.functionClassSymbolName(Name.identifier(name + it)) }
        }.map { it.hashMangle }
        .toSet()
}
