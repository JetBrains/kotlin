/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.descriptors.externalSymbolOrThrow
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationStringValue
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.ir.isUnit
import org.jetbrains.kotlin.backend.konan.isExternalObjCClass
import org.jetbrains.kotlin.backend.konan.isKotlinObjCClass
import org.jetbrains.kotlin.backend.konan.serialization.AbstractKonanIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.uniqueName


// This file describes the ABI for Kotlin descriptors of exported declarations.
// TODO: revise the naming scheme to ensure it produces unique names.
// TODO: do not serialize descriptors of non-exported declarations.

object KonanBinaryInterface {
    private val mangler = object : AbstractKonanIrMangler(true) {}

    private val exportChecker = mangler.getExportChecker()

    val IrFunction.functionName: String get() = mangler.run { signatureString }

    val IrFunction.symbolName: String get() = funSymbolNameImpl()
    val IrField.symbolName: String get() =
        withPrefix(MangleConstant.FIELD_PREFIX, fieldSymbolNameImpl())
    val IrClass.typeInfoSymbolName: String get() =
        withPrefix(MangleConstant.CLASS_PREFIX, typeInfoSymbolNameImpl())
    fun isExported(declaration: IrDeclaration) = exportChecker.run {
        check(declaration, SpecialDeclarationType.REGULAR) || declaration.isPlatformSpecificExported()
    }

    private fun withPrefix(prefix: String, mangle: String) = "$prefix:$mangle"

    private fun IrFunction.funSymbolNameImpl(): String {
        if (!isExported(this)) {
            throw AssertionError(render())
        }

        if (isExternal) {
            this.externalSymbolOrThrow()?.let {
                return it
            }
        }

        this.annotations.findAnnotation(RuntimeNames.exportForCppRuntime)?.let {
            val name = it.getAnnotationStringValue() ?: this.name.asString()
            return name // no wrapping currently required
        }

        return withPrefix(MangleConstant.FUN_PREFIX, mangler.run { mangleString })
    }

    private fun IrField.fieldSymbolNameImpl(): String {
        val containingDeclarationPart = parent.fqNameForIrSerialization.let {
            if (it.isRoot) "" else "$it."
        }
        return "$containingDeclarationPart$name"
    }

    private fun IrClass.typeInfoSymbolNameImpl(): String {
        return this.fqNameForIrSerialization.toString()
    }
}

internal val IrClass.writableTypeInfoSymbolName: String
    get() {
        assert (this.isExported())
        return "ktypew:" + this.fqNameForIrSerialization.toString()
    }

internal val IrClass.globalObjectStorageSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjref:$fqNameForIrSerialization"
    }

internal val IrClass.threadLocalObjectStorageGetterSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjget:$fqNameForIrSerialization"
    }

internal val IrClass.kotlinObjCClassInfoSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.isKotlinObjCClass())

        return "kobjcclassinfo:$fqNameForIrSerialization"
    }

val IrFunction.functionName get() = with(KonanBinaryInterface) { functionName }

val IrFunction.symbolName get() = with(KonanBinaryInterface) { symbolName }

val IrField.symbolName get() = with(KonanBinaryInterface) { symbolName }

val IrClass.typeInfoSymbolName get() = with(KonanBinaryInterface) { typeInfoSymbolName }

fun IrDeclaration.isExported() = KonanBinaryInterface.isExported(this)

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

    return functionType(returnType, isVarArg = false, paramTypes = paramTypes.toTypedArray())
}

internal val IrClass.typeInfoHasVtableAttached: Boolean
    get() = !this.isAbstract() && !this.isExternalObjCClass()

internal val String.moduleConstructorName
    get() = "_Konan_init_${this}"

internal val KonanLibrary.moduleConstructorName
    get() = uniqueName.moduleConstructorName
