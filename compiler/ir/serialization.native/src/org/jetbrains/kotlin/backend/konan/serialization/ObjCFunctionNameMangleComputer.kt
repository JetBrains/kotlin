/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.PlatformSpecificFunctionNameMangleComputer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.getOwnerIfBound
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.native.interop.ObjCMethodInfo

private const val OBJC_MARK = "objc:"
private const val OBJC_CONSTRUCTOR_MARK = "#Constructor"
private const val OBJC_PROPERTY_ACCESSOR_MARK = "#Accessor"

abstract class ObjCFunctionNameMangleComputer<ValueParameter : Any> : PlatformSpecificFunctionNameMangleComputer<ValueParameter> {

    abstract fun getObjCMethodInfo(): ObjCMethodInfo?

    abstract fun getExtensionReceiverClassName(): Name?

    abstract fun isObjCConstructor(): Boolean

    abstract fun isPropertyAccessor(): Boolean

    abstract fun hasObjCMethodAnnotation(): Boolean

    abstract fun hasObjCFactoryAnnotation(): Boolean

    abstract fun isObjCClassMethod(): Boolean

    abstract fun getValueParameterName(valueParameter: ValueParameter): Name

    final override fun computePlatformSpecificFunctionName(): String? {
        val objcMethodInfo = getObjCMethodInfo() ?: return null
        return buildString {
            getExtensionReceiverClassName()?.let {
                append(it)
                append(MangleConstant.FQN_SEPARATOR)
            }
            append(OBJC_MARK)
            append(objcMethodInfo.selector)
            if (isObjCConstructor()) {
                append(OBJC_CONSTRUCTOR_MARK)
            }
            if (isPropertyAccessor()) {
                append(OBJC_PROPERTY_ACCESSOR_MARK)
            }
        }
    }

    final override fun computePlatformSpecificValueParameterPrefix(valueParameter: ValueParameter): String =
        if (hasObjCMethodAnnotation() || hasObjCFactoryAnnotation() || isObjCClassMethod()) {
            "${getValueParameterName(valueParameter)}:"
        } else {
            ""
        }
}

@ObsoleteDescriptorBasedAPI
class DescriptorObjCFunctionNameMangleComputer(
    private val function: FunctionDescriptor
) : ObjCFunctionNameMangleComputer<ParameterDescriptor>() {

    override fun getObjCMethodInfo(): ObjCMethodInfo? =
        (if (function is ConstructorDescriptor && function.isObjCConstructor) function.getObjCInitMethod() else function)
            ?.getObjCMethodInfo()

    override fun getExtensionReceiverClassName(): Name? =
        function.extensionReceiverParameter?.run { type.constructor.declarationDescriptor!!.name }

    override fun isObjCConstructor(): Boolean =
        function is ConstructorDescriptor && function.isObjCConstructor

    override fun isPropertyAccessor(): Boolean =
        function is PropertyAccessorDescriptor

    override fun hasObjCMethodAnnotation(): Boolean = function.annotations.hasAnnotation(objCMethodFqName)

    override fun hasObjCFactoryAnnotation(): Boolean = function.annotations.hasAnnotation(objCFactoryFqName)

    override fun isObjCClassMethod(): Boolean = function.containingDeclaration.let { it is ClassDescriptor && it.isObjCClass() }

    override fun getValueParameterName(valueParameter: ParameterDescriptor): Name = valueParameter.name
}

class IrObjCFunctionNameMangleComputer(private val function: IrFunction) : ObjCFunctionNameMangleComputer<IrValueParameter>() {
    override fun getObjCMethodInfo(): ObjCMethodInfo? =
        (if (function is IrConstructor && function.isObjCConstructor) function.getObjCInitMethod() else function)?.getObjCMethodInfo()

    override fun getExtensionReceiverClassName(): Name? =
        function.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type?.classOrFail
            ?.let { it.getOwnerIfBound()?.name ?: Name.identifier((it.signature as IdSignature.CommonSignature).shortName) }

    override fun isObjCConstructor(): Boolean =
        function is IrConstructor && function.isObjCConstructor

    override fun isPropertyAccessor(): Boolean =
        (function as? IrSimpleFunction)?.correspondingPropertySymbol != null

    override fun hasObjCMethodAnnotation(): Boolean = function.hasAnnotation(NativeStandardInteropNames.objCMethodClassId)

    override fun hasObjCFactoryAnnotation(): Boolean = function.hasAnnotation(NativeStandardInteropNames.objCFactoryClassId)

    override fun isObjCClassMethod(): Boolean =
        // Signatures may be computed during linkage of C-interop Klibs, while not all symbols are linked yet.
        // However, this specific case is supported in a way that does not require bound symbols,
        // so we may pass allowUnboundSymbols = true.
        function.isObjCClassMethod(allowUnboundSymbols = true)

    override fun getValueParameterName(valueParameter: IrValueParameter): Name = valueParameter.name
}
