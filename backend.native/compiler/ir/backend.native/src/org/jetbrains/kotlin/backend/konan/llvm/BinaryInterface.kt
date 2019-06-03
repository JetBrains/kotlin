/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.serialization.KotlinMangler
import org.jetbrains.kotlin.backend.common.serialization.KotlinManglerImpl
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.externalSymbolOrThrow
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationValue
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.KonanMangler.isExported
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.konan.library.uniqueName
import org.jetbrains.kotlin.backend.konan.isInlinedNative
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


// This file describes the ABI for Kotlin descriptors of exported declarations.
// TODO: revise the naming scheme to ensure it produces unique names.
// TODO: do not serialize descriptors of non-exported declarations.

object KonanMangler : KotlinManglerImpl() {

    override val IrType.isInlined
        get() = this.isInlinedNative()

    override val String.hashMangle get() = this.localHash.value

    /**
     * Defines whether the declaration is exported, i.e. visible from other modules.
     *
     * Exported declarations must have predictable and stable ABI
     * that doesn't depend on any internal transformations (e.g. IR lowering),
     * and so should be computable from the descriptor itself without checking a backend state.
     */
    override fun IrDeclaration.isPlatformSpecificExported(): Boolean {
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

        return false
    }

    private val symbolNameAnnotation = RuntimeNames.symbolName

    private val cnameAnnotation = FqName("kotlin.native.CName")

    private val exportForCppRuntimeAnnotation = RuntimeNames.exportForCppRuntime

    private val exportForCompilerAnnotation = RuntimeNames.exportForCompilerAnnotation


    override val IrFunction.argsPart get() = this.valueParameters.map {

        // TODO: there are clashes originating from ObjectiveC interop.
        // kotlinx.cinterop.ObjCClassOf<T>.create(format: kotlin.String): T defined in platform.Foundation in file Foundation.kt
        // and
        // kotlinx.cinterop.ObjCClassOf<T>.create(string: kotlin.String): T defined in platform.Foundation in file Foundation.kt

        val argName =
                if (this.hasObjCMethodAnnotation || this.hasObjCFactoryAnnotation || this.isObjCClassMethod()) "${it.name}:" else ""
        "$argName${typeToHashString(it.type)}${if (it.isVararg) "_VarArg" else ""}"
    }.joinToString(";")


    override val IrFunction.platformSpecificFunctionName: String?
        get() {
            (if (this is IrConstructor && this.isObjCConstructor) this.getObjCInitMethod() else this)?.getObjCMethodInfo()
                ?.let {
                    return buildString {
                        if (extensionReceiverParameter != null) {
                            append(extensionReceiverParameter!!.type.getClass()!!.name)
                            append(".")
                        }

                        append("objc:")
                        append(it.selector)
                        if (this@platformSpecificFunctionName is IrConstructor && this@platformSpecificFunctionName.isObjCConstructor) append("#Constructor")

                        if ((this@platformSpecificFunctionName as? IrSimpleFunction)?.correspondingProperty != null) {
                            append("#Accessor")
                        }
                    }
                }
            return null
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

            val containingDeclarationPart = parent.fqNameForIrSerialization.let {
                if (it.isRoot) "" else "$it."
            }
            return "kfun:$containingDeclarationPart$functionName"
        }
}

internal val IrClass.writableTypeInfoSymbolName: String
    get() {
        assert (this.isExported())
        return "ktypew:" + this.fqNameForIrSerialization.toString()
    }

internal val theUnitInstanceName = "kobj:kotlin.Unit"

internal val IrClass.objectInstanceFieldSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjref:$fqNameForIrSerialization"
    }

internal val IrClass.objectInstanceShadowFieldSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())
        assert (this.objectIsShared)

        return "kshadowobjref:$fqNameForIrSerialization"
    }

val IrFunction.functionName get() = with(KonanMangler) { functionName }

val IrFunction.symbolName get() = with(KonanMangler) { symbolName }

val IrField.symbolName get() = with(KonanMangler) { symbolName }

val IrClass.typeInfoSymbolName get() = with(KonanMangler) { typeInfoSymbolName }

fun IrDeclaration.isExported() = with(KonanMangler) { isExported() }

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

internal val IrClass.typeInfoHasVtableAttached: Boolean
    get() = !this.isAbstract() && !this.isExternalObjCClass()

internal fun ModuleDescriptor.privateFunctionSymbolName(index: Int, functionName: String?) = "private_functions_${name.asString()}_${functionName}_$index"

internal fun ModuleDescriptor.privateClassSymbolName(index: Int, className: String?) = "private_classes_${name.asString()}_${className}_$index"

internal val String.moduleConstructorName
    get() = "_Konan_init_${this}"

internal val KonanLibrary.moduleConstructorName
    get() = uniqueName.moduleConstructorName
