/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.objcinterop

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedClassConstructorDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.native.interop.ObjCMethodInfo
import org.jetbrains.kotlin.resolve.annotations.getAnnotationStringValue
import org.jetbrains.kotlin.resolve.annotations.getArgumentValueOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.DFS

private fun IrFunction.isFakeOverrideInProgressOfBuilding() = this is IrFunctionWithLateBinding && !isBound && isFakeOverride

internal val interopPackageName = NativeStandardInteropNames.cInteropPackage
internal val objCObjectFqName = NativeStandardInteropNames.objCObjectClassId.asSingleFqName()
private val objCClassFqName = interopPackageName.child(Name.identifier("ObjCClass"))
private val objCProtocolFqName = interopPackageName.child(Name.identifier("ObjCProtocol"))
val externalObjCClassFqName = NativeStandardInteropNames.externalObjCClassClassId.asSingleFqName()
val objCDirectFqName = NativeStandardInteropNames.objCDirectClassId.asSingleFqName()
val objCMethodFqName = NativeStandardInteropNames.objCMethodClassId.asSingleFqName()
val objCConstructorFqName = NativeStandardInteropNames.objCConstructorClassId.asSingleFqName()
val objCFactoryFqName = NativeStandardInteropNames.objCFactoryClassId.asSingleFqName()

fun ClassDescriptor.isObjCClass(): Boolean =
                this.containingDeclaration.fqNameSafe != interopPackageName &&
        this.getAllSuperClassifiers().any { it.fqNameSafe == objCObjectFqName } // TODO: this is not cheap. Cache me!

fun KotlinType.isObjCObjectType(): Boolean =
        (this.supertypes() + this).any { TypeUtils.getClassDescriptor(it)?.fqNameSafe == objCObjectFqName }

private fun IrClass.selfOrAnySuperClass(pred: (IrClass) -> Boolean): Boolean {
    if (pred(this)) return true

    return superTypes.any { it.classOrNull!!.owner.selfOrAnySuperClass(pred) }
}

fun IrClass.isObjCClass() = this.packageFqName != interopPackageName &&
        selfOrAnySuperClass { it.hasEqualFqName(objCObjectFqName) }

fun IrType.isObjCObjectType(): Boolean = DFS.ifAny(
    /* nodes = */ listOf(this.classifierOrFail),
    /* neighbors = */ { current -> current.superTypes().map { it.classifierOrFail } },
    /* predicate = */ { (it as? IrClassSymbol)?.owner?.hasEqualFqName(objCObjectFqName) == true }
)

fun ClassDescriptor.isExternalObjCClass(): Boolean = this.isObjCClass() &&
        this.parentsWithSelf.filterIsInstance<ClassDescriptor>().any {
            it.annotations.findAnnotation(externalObjCClassFqName) != null
        }
fun IrClass.isExternalObjCClass(): Boolean = this.isObjCClass() &&
        this.parentDeclarationsWithSelf.filterIsInstance<IrClass>().any {
            it.annotations.hasAnnotation(externalObjCClassFqName)
        }

fun ClassDescriptor.isObjCForwardDeclaration(): Boolean = when (NativeForwardDeclarationKind.packageFqNameToKind[findPackage().fqName]) {
    null, NativeForwardDeclarationKind.Struct -> false
    NativeForwardDeclarationKind.ObjCProtocol, NativeForwardDeclarationKind.ObjCClass -> true
}

fun IrClass.isObjCForwardDeclaration(): Boolean = when (NativeForwardDeclarationKind.packageFqNameToKind[getPackageFragment().packageFqName]) {
    null, NativeForwardDeclarationKind.Struct -> false
    NativeForwardDeclarationKind.ObjCProtocol, NativeForwardDeclarationKind.ObjCClass -> true
}


fun ClassDescriptor.isObjCMetaClass(): Boolean = this.getAllSuperClassifiers().any {
    it.fqNameSafe == objCClassFqName
}

fun IrClass.isObjCMetaClass(): Boolean = selfOrAnySuperClass {
    it.hasEqualFqName(objCClassFqName)
}

fun IrClass.isObjCProtocolClass(): Boolean = hasEqualFqName(objCProtocolFqName)

fun ClassDescriptor.isObjCProtocolClass(): Boolean =
        this.fqNameSafe == objCProtocolFqName

fun IrFunction.isObjCClassMethod() =
        this.parent.let { it is IrClass && it.isObjCClass() }

fun IrFunction.isExternalObjCClassMethod() =
    this.parent.let {it is IrClass && it.isExternalObjCClass()}

fun IrFunction.canObjCClassMethodBeCalledVirtually(overridden: IrFunction) =
    overridden.isOverridable && !this.isFakeOverride && !this.isExternalObjCClassMethod()

fun ClassDescriptor.isKotlinObjCClass(): Boolean = this.isObjCClass() && !this.isExternalObjCClass()

fun IrClass.isKotlinObjCClass(): Boolean = this.isObjCClass() && !this.isExternalObjCClass()


private fun FunctionDescriptor.decodeObjCMethodAnnotation(): ObjCMethodInfo? {
    assert (this.kind.isReal)

    val methodInfo = this.annotations.findAnnotation(objCMethodFqName)?.let {
        ObjCMethodInfo(
                selector = it.getAnnotationStringValue("selector"),
                encoding = it.getAnnotationStringValue("encoding"),
                isStret = it.getArgumentValueOrNull<Boolean>("isStret") ?: false,
                directSymbol = this.annotations.findAnnotation(objCDirectFqName)?.getAnnotationStringValue("symbol"),
        )
    }

    return methodInfo
}

private fun IrFunction.decodeObjCMethodAnnotation(): ObjCMethodInfo? {
    require(this.isReal || this.isFakeOverrideInProgressOfBuilding())

    val methodInfo = this.annotations.findAnnotation(objCMethodFqName)?.let {
        ObjCMethodInfo(
                selector = it.getAnnotationStringValue("selector"),
                encoding = it.getAnnotationStringValue("encoding"),
                isStret = it.getAnnotationValueOrNull<Boolean>("isStret") ?: false,
                directSymbol = this.annotations.findAnnotation(objCDirectFqName)?.getAnnotationStringValue("symbol"),
        )
    }

    return methodInfo
}

private fun objCMethodInfo(annotation: IrConstructorCall) = ObjCMethodInfo(
        selector = annotation.getAnnotationStringValue("selector"),
        encoding = annotation.getAnnotationStringValue("encoding"),
        isStret = annotation.getAnnotationValueOrNull<Boolean>("isStret") ?: false,
        directSymbol = null,
)

/**
 * @param onlyExternal indicates whether to accept overriding methods from Kotlin classes
 */
private fun FunctionDescriptor.getObjCMethodInfo(onlyExternal: Boolean): ObjCMethodInfo? {
    if (this.kind.isReal) {
        this.decodeObjCMethodAnnotation()?.let { return it }

        if (onlyExternal) {
            return null
        }
    }

    return overriddenDescriptors.firstNotNullOfOrNull { it.getObjCMethodInfo(onlyExternal) }
}

/**
 * @param onlyExternal indicates whether to accept overriding methods from Kotlin classes
 */
private fun IrSimpleFunction.getObjCMethodInfo(onlyExternal: Boolean): ObjCMethodInfo? {
    // During fake override building we need this method, but overriddenSymbols are not valid yet.
    // So let's pretend this override is real. It has annotation copied, if it exists
    if (this.isFakeOverrideInProgressOfBuilding()) {
        decodeObjCMethodAnnotation()?.let { return it }
    }
    if (this.isReal) {
        this.decodeObjCMethodAnnotation()?.let { return it }

        if (onlyExternal) {
            return null
        }
    }

    return overriddenSymbols.firstNotNullOfOrNull {
        assert(it.owner != this) { "Function ${it.owner.fqNameWhenAvailable}() is wrongly contained in its own overriddenSymbols"}
        it.owner.getObjCMethodInfo(onlyExternal)
    }
}

fun FunctionDescriptor.getExternalObjCMethodInfo(): ObjCMethodInfo? = this.getObjCMethodInfo(onlyExternal = true)

fun IrFunction.getExternalObjCMethodInfo(): ObjCMethodInfo? = (this as? IrSimpleFunction)?.getObjCMethodInfo(onlyExternal = true)

fun FunctionDescriptor.getObjCMethodInfo(): ObjCMethodInfo? = this.getObjCMethodInfo(onlyExternal = false)

fun IrFunction.getObjCMethodInfo(): ObjCMethodInfo? = (this as? IrSimpleFunction)?.getObjCMethodInfo(onlyExternal = false)

fun IrFunction.isObjCBridgeBased(): Boolean {
    assert(this.isReal)

    return this.annotations.hasAnnotation(objCMethodFqName) ||
            this.annotations.hasAnnotation(objCFactoryFqName) ||
            this.annotations.hasAnnotation(objCConstructorFqName)
}

fun IrConstructor.objCConstructorIsDesignated(): Boolean =
    this.getAnnotationArgumentValue<Boolean>(objCConstructorFqName, "designated")
        ?: error("Could not find 'designated' argument")


val IrConstructor.isObjCConstructor get() = this.annotations.hasAnnotation(objCConstructorFqName)
val ConstructorDescriptor.isObjCConstructor get() = this.annotations.hasAnnotation(objCConstructorFqName)

// TODO-DCE-OBJC-INIT: Selector should be preserved by DCE.
fun IrConstructor.getObjCInitMethod(): IrSimpleFunction? {
    return this.annotations.findAnnotation(objCConstructorFqName)?.let {
        val initSelector = it.getAnnotationStringValue("initSelector")
        this.constructedClass.declarations.asSequence()
                .filterIsInstance<IrSimpleFunction>()
                .single { it.getExternalObjCMethodInfo()?.selector == initSelector }
    }
}

@ObsoleteDescriptorBasedAPI
fun ConstructorDescriptor.getObjCInitMethod(): FunctionDescriptor? {
    if (this is IrBasedClassConstructorDescriptor) {
        // E.g. in case of K2.
        // The constructedClass has empty member scope, so we have to delegate to IR to find the init method.
        return this.owner.getObjCInitMethod()?.descriptor
    }

    return this.annotations.findAnnotation(objCConstructorFqName)?.let {
        val initSelector = it.getAnnotationStringValue("initSelector")
        val memberScope = constructedClass.unsubstitutedMemberScope
        val functionNames = memberScope.getFunctionNames()
        for (name in functionNames) {
            val functions = memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
            for (function in functions) {
                val objectInfo = function.getExternalObjCMethodInfo() ?: continue
                if (objectInfo.selector == initSelector) return function
            }
        }
        error("Cannot find ObjInitMethod for $this")
    }
}

fun IrFunction.getObjCFactoryInitMethodInfo(): ObjCMethodInfo? {
    val factoryAnnotation = this.annotations.findAnnotation(objCFactoryFqName) ?: return null
    return objCMethodInfo(factoryAnnotation)
}

fun inferObjCSelector(descriptor: FunctionDescriptor): String = if (descriptor.valueParameters.isEmpty()) {
    descriptor.name.asString()
} else {
    buildString {
        append(descriptor.name)
        append(':')
        descriptor.valueParameters.drop(1).forEach {
            append(it.name)
            append(':')
        }
    }
}

fun IrClass.getExternalObjCClassBinaryName(): String =
        this.getExplicitExternalObjCClassBinaryName()
                ?: this.name.asString()

fun IrClass.getExternalObjCMetaClassBinaryName(): String =
        this.getExplicitExternalObjCClassBinaryName()
                ?: this.name.asString().removeSuffix("Meta")

private fun IrClass.getExplicitExternalObjCClassBinaryName() =
        this.annotations.findAnnotation(externalObjCClassFqName)!!.getAnnotationValueOrNull<String>("binaryName")
