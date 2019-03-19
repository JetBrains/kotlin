/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isInlined
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

interface KotlinMangler {
    val String.hashMangle: Long
    val IrDeclaration.hashedMangle: Long
    fun IrDeclaration.isExported(): Boolean
    val IrFunction.functionName: String

}

abstract class KotlinManglerImpl: KotlinMangler {
    override val IrDeclaration.hashedMangle: Long
        get() = this.uniqSymbolName().hashMangle


    // We can't call "with (super) { this.isExported() }" in children.
    // So provide a hook.
    protected open fun IrDeclaration.isPlatformSpecificExported(): Boolean = false

    /**
     * Defines whether the declaration is exported, i.e. visible from other modules.
     *
     * Exported declarations must have predictable and stable ABI
     * that doesn't depend on any internal transformations (e.g. IR lowering),
     * and so should be computable from the descriptor itself without checking a backend state.
     */
    override tailrec fun IrDeclaration.isExported(): Boolean {
        // TODO: revise
        val descriptorAnnotations = this.descriptor.annotations

        if (this.isPlatformSpecificExported()) return true

        if (descriptorAnnotations.hasAnnotation(publishedApiAnnotation)) {
            return true
        }

        if (this.isAnonymousObject)
            return false

        if (this is IrConstructor && constructedClass.kind.isSingleton) {
            // Currently code generator can access the constructor of the singleton,
            // so ignore visibility of the constructor itself.
            return constructedClass.isExported()
        }

        if (this is IrFunction) {
            val descriptor = this.descriptor
            // TODO: this code is required because accessor doesn't have a reference to property.
            if (descriptor is PropertyAccessorDescriptor) {
                val property = descriptor.correspondingProperty
                if (property.annotations.hasAnnotation(publishedApiAnnotation)) return true
            }
        }

        val visibility = when (this) {
            is IrClass -> this.visibility
            is IrFunction -> this.visibility
            is IrProperty -> this.visibility
            is IrField -> this.visibility
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

        val parent = this.parent
        if (parent is IrDeclaration) {
            return parent.isExported()
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

        var hashString = type.getClass()?.run { fqNameSafe.asString() } ?: "<dynamic>"

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

    open val IrFunction.argsPart get() = this.valueParameters.map {
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
                    returnType.isInlined() -> "ValueType"
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
            this.platformSpecificFunctionName ?. let { return it }
            val name = this.name.mangleIfInternal(this.module, this.visibility)
            return "$name$signature"
        }

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
            val containingDeclarationPart = parent.fqNameSafe.let {
                if (it.isRoot) "" else "$it."
            }
            return "kfield:$containingDeclarationPart$name"

        }

    val IrClass.typeInfoSymbolName: String
        get() {
            assert(this.isExported())
            return "ktype:" + this.fqNameSafe.toString()
        }

    val IrTypeParameter.symbolName: String
        get() {
            val containingDeclarationPart = parent.fqNameSafe
            return "ktypeparam:$containingDeclarationPart$name"
        }

// This is a little extension over what's used in real mangling
// since some declarations never appear in the bitcode symbols.

    internal fun IrDeclaration.uniqSymbolName(): String = when (this) {
        is IrFunction
        -> this.uniqFunctionName
        is IrProperty
        -> this.symbolName
        is IrClass
        -> this.typeInfoSymbolName
        is IrField
        -> this.symbolName
        is IrEnumEntry
        -> this.symbolName
        is IrTypeParameter
        -> this.symbolName
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
            else -> this.name
        }

    private val IrProperty.symbolName: String
        get() {
            val extensionReceiver: String = getter!!.extensionReceiverParameter?.extensionReceiverNamePart ?: ""

            val containingDeclarationPart = parent.fqNameSafe.let {
                if (it.isRoot) "" else "$it."
            }
            return "kprop:$containingDeclarationPart$extensionReceiver$name"
        }

    private val IrEnumEntry.symbolName: String
        get() {
            val containingDeclarationPart = parent.fqNameSafe.let {
                if (it.isRoot) "" else "$it."
            }
            return "kenumentry:$containingDeclarationPart$name"
        }

    // This is basicly the same as .symbolName, but disambiguates external functions with the same C name.
// In addition functions appearing in fq sequence appear as <full signature>.
    private val IrFunction.uniqFunctionName: String
        get() {
            val parent = this.parent

            val containingDeclarationPart = parent.fqNameUnique.let {
                if (it.isRoot) "" else "$it."
            }

            return "kfun:$containingDeclarationPart#$functionName"
        }


}
