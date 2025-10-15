/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cgen

import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.isAny
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.objcinterop.isObjCObjectType
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

fun IrType.isCEnumType(): Boolean {
    if (isNullable()) return false
    val enumClass = classOrNull?.owner ?: return false
    if (!enumClass.isEnumClass) return false

    return enumClass.superTypes
            .any { (it.classifierOrNull?.owner as? IrClass)?.fqNameForIrSerialization == FqName("kotlinx.cinterop.CEnum") }
}

private val cCall = RuntimeNames.cCall

fun IrFunction.isCFunctionOrGlobalAccessor(): Boolean =
        annotations.hasAnnotation(RuntimeNames.cCall)
                || annotations.hasAnnotation(RuntimeNames.cCallDirect)

fun IrDeclaration.hasCCallAnnotation(name: String): Boolean =
        this.annotations.hasAnnotation(cCall.child(Name.identifier(name)))

fun IrValueParameter.isWCStringParameter() = hasCCallAnnotation("WCString")

fun IrValueParameter.isCStringParameter() = hasCCallAnnotation("CString")

fun IrValueParameter.isObjCConsumed() = hasCCallAnnotation("Consumed")

fun IrSimpleFunction.objCConsumesReceiver() = hasCCallAnnotation("ConsumesReceiver")

fun IrSimpleFunction.objCReturnsRetained() = hasCCallAnnotation("ReturnsRetained")

fun IrClass.getCStructSpelling(): String? =
        getAnnotationArgumentValue(FqName("kotlinx.cinterop.internal.CStruct"), "spelling")

fun IrType.isTypeOfNullLiteral(): Boolean = isNullableNothing()

fun IrType.isVector(): Boolean {
    if (this is IrSimpleType && !this.isNullable()) {
        return classifier.isClassWithFqName(KonanFqNames.Vector128.toUnsafe())
    }
    return false
}

fun IrType.isObjCReferenceType(target: KonanTarget, irBuiltIns: IrBuiltIns): Boolean {
    if (!target.family.isAppleFamily) return false

    // Handle the same types as produced by [objCPointerMirror] in Interop/StubGenerator/.../Mappings.kt.

    if (isObjCObjectType()) return true

    return when (classifierOrNull) {
        irBuiltIns.anyClass,
        irBuiltIns.stringClass,
        irBuiltIns.listClass,
        irBuiltIns.mutableListClass,
        irBuiltIns.setClass,
        irBuiltIns.mapClass -> true
        else -> false
    }
}

fun IrType.isCPointer(symbols: KonanSymbols): Boolean = this.classOrNull == symbols.interopCPointer
fun IrType.isCValue(symbols: KonanSymbols): Boolean = this.classOrNull == symbols.interopCValue
fun IrType.isCValuesRef(symbols: KonanSymbols): Boolean = this.classOrNull == symbols.interopCValuesRef

fun IrType.isNativePointed(symbols: KonanSymbols): Boolean = isSubtypeOfClass(symbols.nativePointed)

fun IrType.isCStructFieldTypeStoredInMemoryDirectly(): Boolean = isPrimitiveType() || isUnsigned() || isVector()

fun IrType.isCStructFieldSupportedReferenceType(symbols: KonanSymbols): Boolean =
        isObjCObjectType()
                || getClass()?.isAny() == true
                || isStringClassType()
                || classOrNull == symbols.list
                || classOrNull == symbols.mutableList
                || classOrNull == symbols.set
                || classOrNull == symbols.map

/**
 * Check given function is a getter or setter
 * for `value` property of CEnumVar subclass.
 */
fun IrFunction.isCEnumVarValueAccessor(symbols: KonanSymbols): Boolean {
    val parent = parent as? IrClass ?: return false
    return if (symbols.interopCEnumVar in parent.superClasses && isPropertyAccessor) {
        (propertyIfAccessor as IrProperty).name.asString() == "value"
    } else {
        false
    }
}

fun IrFunction.isCStructMemberAtAccessor() = hasAnnotation(RuntimeNames.cStructMemberAt)

fun IrFunction.isCStructArrayMemberAtAccessor() = hasAnnotation(RuntimeNames.cStructArrayMemberAt)

fun IrFunction.isCStructBitFieldAccessor() = hasAnnotation(RuntimeNames.cStructBitField)

// TODO: rework Boolean support.
// TODO: What should be used on watchOS?
fun cBoolType(target: KonanTarget): CType? = when (target.family) {
    Family.IOS, Family.TVOS, Family.WATCHOS -> CTypes.C99Bool
    else -> CTypes.signedChar
}