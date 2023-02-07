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
import org.jetbrains.kotlin.ir.backend.web.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.IrType
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
    return (parent as? IrFile)?.getJsQualifier()?.let { FqName(it) } ?: parent.fqName.takeIf { shouldIncludePackage }
}

val IrFunctionAccessExpression.typeArguments: List<IrType?>
    get() = List(typeArgumentsCount) { getTypeArgument(it) }

val IrFunctionAccessExpression.valueArguments: List<IrExpression?>
    get() = List(valueArgumentsCount) { getValueArgument(it) }
val IrClass.isInstantiableEnum: Boolean
    get() = isEnumClass && !isExpect && !isEffectivelyExternal()

val IrDeclaration.parentEnumClassOrNull: IrClass?
    get() = parents.filterIsInstance<IrClass>().firstOrNull { it.isInstantiableEnum }

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

