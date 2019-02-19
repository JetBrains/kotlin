/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.externalSymbolOrThrow
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationValue
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.konan.library.uniqueName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


// This file describes the ABI for Kotlin descriptors of exported declarations.
// TODO: revise the naming scheme to ensure it produces unique names.
// TODO: do not serialize descriptors of non-exported declarations.

/**
 * Defines whether the declaration is exported, i.e. visible from other modules.
 *
 * Exported declarations must have predictable and stable ABI
 * that doesn't depend on any internal transformations (e.g. IR lowering),
 * and so should be computable from the descriptor itself without checking a backend state.
 */
internal tailrec fun IrDeclaration.isExported(): Boolean {
    // TODO: revise
    val descriptorAnnotations = this.descriptor.annotations
    if (descriptorAnnotations.hasAnnotation(symbolNameAnnotation)) {
        // Treat any `@SymbolName` declaration as exported.
        return true
    }
    if (descriptorAnnotations.hasAnnotation(exportForCppRuntimeAnnotation)) {
        // Treat any `@ExportForCppRuntime` declaration as exported.
        return true
    }
    if (descriptorAnnotations.hasAnnotation(cnameAnnotation)) {
        // Treat `@CName` declaration as exported.
        return true
    }
    if (descriptorAnnotations.hasAnnotation(exportForCompilerAnnotation)) {
        return true
    }
    if (descriptorAnnotations.hasAnnotation(publishedApiAnnotation)){
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

private val symbolNameAnnotation = RuntimeNames.symbolName

private val cnameAnnotation = FqName("kotlin.native.CName")

private val exportForCppRuntimeAnnotation = RuntimeNames.exportForCppRuntime

private val exportForCompilerAnnotation = RuntimeNames.exportForCompilerAnnotation

private val publishedApiAnnotation = FqName("kotlin.PublishedApi")

private fun acyclicTypeMangler(visited: MutableSet<IrTypeParameter>, type: IrType): String {
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

    var hashString = type.getClass()!!.fqNameSafe.asString()
    if (type !is IrSimpleType) error(type)
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
    return hashString
}

private fun typeToHashString(type: IrType)
    = acyclicTypeMangler(mutableSetOf<IrTypeParameter>(), type)

internal val IrValueParameter.extensionReceiverNamePart: String
    get() = "@${typeToHashString(this.type)}."

private val IrFunction.signature: String
    get() {
        val extensionReceiverPart = this.extensionReceiverParameter?.extensionReceiverNamePart ?: ""
        val argsPart = this.valueParameters.map {

        // TODO: there are clashes originating from ObjectiveC interop.
        // kotlinx.cinterop.ObjCClassOf<T>.create(format: kotlin.String): T defined in platform.Foundation in file Foundation.kt
        // and
        // kotlinx.cinterop.ObjCClassOf<T>.create(string: kotlin.String): T defined in platform.Foundation in file Foundation.kt

            val argName = if (this.hasObjCMethodAnnotation || this.hasObjCFactoryAnnotation || this.isObjCClassMethod()) "${it.name}:" else ""
            "$argName${typeToHashString(it.type)}${if (it.isVararg) "_VarArg" else ""}"
        }.joinToString(";")
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

// TODO: rename to indicate that it has signature included
internal val IrFunction.functionName: String
    get() {
        (if (this is IrConstructor && this.isObjCConstructor) this.getObjCInitMethod() else this)?.getObjCMethodInfo()?.let {
            return buildString {
                if (extensionReceiverParameter != null) {
                    append(extensionReceiverParameter!!.type.getClass()!!.name)
                    append(".")
                }

                append("objc:")
                append(it.selector)
                if (this@functionName is IrConstructor && this@functionName.isObjCConstructor) append("#Constructor")

                // We happen to have the clashing combinations such as
                //@ObjCMethod("issueChallengeToPlayers:message:", "objcKniBridge1165")
                //external fun GKScore.issueChallengeToPlayers(playerIDs: List<*>?, message: String?): Unit
                //@ObjCMethod("issueChallengeToPlayers:message:", "objcKniBridge1172")
                //external fun GKScore.issueChallengeToPlayers(playerIDs: List<*>?, message: String?): Unit
                // So disambiguate by the name of the bridge for now.
                // TODO: idealy we'd never generate such identical declarations.

                if (this@functionName is IrSimpleFunction && this@functionName.hasObjCMethodAnnotation()) {
                    this@functionName.objCMethodArgValue("selector") ?.let { append("#$it") }
                    this@functionName.objCMethodArgValue("bridge") ?.let { append("#$it") }
                }
            }
        }

        val name = this.name.mangleIfInternal(this.module, this.visibility)

        return "$name$signature"
    }

private fun Name.mangleIfInternal(moduleDescriptor: ModuleDescriptor, visibility: Visibility): String =
        if (visibility != Visibilities.INTERNAL) {
            this.asString()
        } else {
            val moduleName = moduleDescriptor.name.asString()
                    .let { it.substring(1, it.lastIndex) } // Remove < and >.

            "$this\$$moduleName"
        }

internal val IrFunction.symbolName: String
    get() {
        if (!this.isExported()) {
            throw AssertionError(this.descriptor.toString())
        }

        if (isExternal) {
            this.descriptor.externalSymbolOrThrow()?.let {
                return it
            }
        }

        this.descriptor.annotations.findAnnotation(exportForCppRuntimeAnnotation)?.let {
            val name = getAnnotationValue(it) ?: this.name.asString()
            return name // no wrapping currently required
        }

        val parent = this.parent

        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kfun:$containingDeclarationPart$functionName"
    }

internal val IrField.symbolName: String
    get() {
        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kfield:$containingDeclarationPart$name"

    }

// TODO: bring here dependencies of this method?
internal fun RuntimeAware.getLlvmFunctionType(function: IrFunction): LLVMTypeRef {
    val returnType = when {
        function is IrConstructor -> voidType
        function.isSuspend -> kObjHeaderPtr                // Suspend functions return Any?.
        else -> getLLVMReturnType(function.returnType)
    }
    val paramTypes = ArrayList(function.allParameters.map { getLLVMType(it.type) })
    if (function.isSuspend)
        paramTypes.add(kObjHeaderPtr)                       // Suspend functions have implicit parameter of type Continuation<>.
    if (isObjectType(returnType)) paramTypes.add(kObjHeaderPtrPtr)

    return functionType(returnType, isVarArg = false, paramTypes = *paramTypes.toTypedArray())
}

internal fun RuntimeAware.getLlvmFunctionType(symbol: DataFlowIR.FunctionSymbol): LLVMTypeRef {
    val returnType = if (symbol.returnsUnit) voidType else getLLVMType(symbol.returnParameter.type)
    val paramTypes = ArrayList(symbol.parameters.map { getLLVMType(it.type) })
    if (isObjectType(returnType)) paramTypes.add(kObjHeaderPtrPtr)

    return functionType(returnType, isVarArg = false, paramTypes = *paramTypes.toTypedArray())
}

internal val IrClass.typeInfoSymbolName: String
    get() {
        assert (this.isExported())
        return "ktype:" + this.fqNameSafe.toString()
    }

internal val IrClass.writableTypeInfoSymbolName: String
    get() {
        assert (this.isExported())
        return "ktypew:" + this.fqNameSafe.toString()
    }

internal val theUnitInstanceName = "kobj:kotlin.Unit"

internal val IrClass.objectInstanceFieldSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjref:$fqNameSafe"
    }

internal val IrClass.objectInstanceShadowFieldSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())
        assert (this.objectIsShared)

        return "kshadowobjref:$fqNameSafe"
    }

internal val IrClass.typeInfoHasVtableAttached: Boolean
    get() = !this.isAbstract() && !this.isExternalObjCClass()

internal fun ModuleDescriptor.privateFunctionSymbolName(index: Int, functionName: String?) = "private_functions_${name.asString()}_${functionName}_$index"

internal fun ModuleDescriptor.privateClassSymbolName(index: Int, className: String?) = "private_classes_${name.asString()}_${className}_$index"

internal val String.moduleConstructorName
    get() = "_Konan_init_${this}"

internal val KonanLibrary.moduleConstructorName
    get() = uniqueName.moduleConstructorName
