/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.descriptors.isClass
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.FqName

fun IrClass.jsConstructorReference(context: JsIrBackendContext): IrExpression {
    return JsIrBuilder.buildCall(context.intrinsics.jsClass, origin = JsStatementOrigins.CLASS_REFERENCE)
        .apply { putTypeArgument(0, defaultType) }
}

fun IrDeclaration.isExportedMember(context: JsIrBackendContext) =
    (this is IrDeclarationWithVisibility && visibility.isPublicAPI) &&
            parentClassOrNull?.isExported(context) == true

fun IrDeclaration?.isExportedClass(context: JsIrBackendContext) =
    this is IrClass && kind.isClass && isExported(context)

fun IrDeclaration?.isExportedInterface(context: JsIrBackendContext) =
    this is IrClass && kind.isInterface && isExported(context)

fun IrReturn.isTheLastReturnStatementIn(target: IrReturnableBlockSymbol): Boolean {
    val ownerFirstStatement = target.owner.statements.singleOrNull()
    if (ownerFirstStatement is IrInlinedFunctionBlock) {
        return ownerFirstStatement.statements.lastOrNull() === this
    }
    return target.owner.statements.lastOrNull() === this
}

fun IrDeclarationWithName.getFqNameWithJsNameWhenAvailable(shouldIncludePackage: Boolean): FqName {
    val name = getJsNameOrKotlinName()
    return when (val parent = parent) {
        is IrDeclarationWithName -> parent.getFqNameWithJsNameWhenAvailable(shouldIncludePackage).child(name)
        is IrPackageFragment -> getKotlinOrJsQualifier(parent, shouldIncludePackage)?.child(name) ?: FqName(name.identifier)
        else -> FqName(name.identifier)
    }
}

fun IrConstructor.hasStrictSignature(context: JsIrBackendContext): Boolean {
    val primitives = with(context.irBuiltIns) { primitiveTypesToPrimitiveArrays.values + stringClass }
    return with(parentAsClass) {
        isExternal || isExpect || isAnnotationClass || context.inlineClassesUtils.isClassInlineLike(this) || symbol in primitives
    }
}

private fun getKotlinOrJsQualifier(parent: IrPackageFragment, shouldIncludePackage: Boolean): FqName? {
    return (parent as? IrFile)?.getJsQualifier()?.let { FqName(it) } ?: parent.packageFqName.takeIf { shouldIncludePackage }
}

val IrFunctionAccessExpression.typeArguments: List<IrType?>
    get() = List(typeArgumentsCount) { getTypeArgument(it) }

val IrFunctionAccessExpression.valueArguments: List<IrExpression?>
    get() = List(valueArgumentsCount) { getValueArgument(it) }

val IrClass.isInstantiableEnum: Boolean
    get() = isEnumClass && !isExpect && !isEffectivelyExternal()

val IrDeclaration.parentEnumClassOrNull: IrClass?
    get() = parents.filterIsInstance<IrClass>().firstOrNull { it.isInstantiableEnum }

fun IrFunctionSymbol.isUnitInstanceFunction(context: JsIrBackendContext): Boolean {
    return owner.origin === JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION &&
            owner.returnType.classifierOrNull === context.irBuiltIns.unitClass
}

// TODO: the code is written to pass Repl tests, so we should understand. why in Repl tests we don't have backingField
fun JsIrBackendContext.getVoid(): IrExpression =
    intrinsics.void.owner.backingField?.let {
        IrGetFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            it.symbol,
            irBuiltIns.nothingNType
        )
    } ?: IrConstImpl.constNull(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        irBuiltIns.nothingNType
    )

fun irEmpty(context: JsIrBackendContext): IrExpression {
    return JsIrBuilder.buildComposite(context.dynamicType, emptyList())
}

fun IrDeclaration.isObjectInstanceGetter(): Boolean {
    return this is IrSimpleFunction && isObjectInstanceGetter()
}

fun IrSimpleFunction.isObjectInstanceGetter(): Boolean {
    return origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION
}

fun IrDeclaration.isObjectInstanceField(): Boolean {
    return this is IrField && isObjectInstanceField()
}

fun IrField.isObjectInstanceField(): Boolean {
    return origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
}
