/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.gcc

import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.export.isMangled
import org.jetbrains.kotlin.ir.backend.js.jsdoc.annotateWithContext
import org.jetbrains.kotlin.ir.backend.js.jsdoc.annotateWithoutContext
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.isInterface
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.overrides.isOverridableMemberOrAccessor
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.js.backend.ast.JsStatement

fun JsStatement.withJsDoc(element: IrElement, context: JsGenerationContext) =
    when (element) {
        is IrField -> withJsDocForField(element, context)
        is IrVariable -> withJsDocForVariable(element, context)
        is IrProperty -> withJsDocForProperty(element, context)
        is IrSetField -> withJsDocForInitializer(element, context)
        is IrSimpleFunction -> withJsDocForFunction(element, context)
        is IrConstructor -> withJsDocForConstructor(element, context)
        else -> this
    }

fun JsStatement.withJsDocForExported() =
    annotateWithoutContext {
        export()
    }

fun JsStatement.withJsDocForField(field: IrField, context: JsGenerationContext) =
    annotateWithContext(context) {
        mutability(isMutable = true, field.type)
    }

fun JsStatement.withJsDocForVariable(variable: IrVariable, context: JsGenerationContext) =
    annotateWithContext(context) {
        if (variable.isConst) constant(variable.type)
        else mutability(variable.isVar, variable.type)
    }

fun JsStatement.withJsDocForInitializer(setField: IrSetField, context: JsGenerationContext) =
    annotateWithContext(context) {
        if (setField.origin !== IrStatementOrigin.INITIALIZE_FIELD) return@annotateWithContext
        val field = setField.symbol.owner
        private()
        mutability(field.correspondingPropertySymbol?.owner?.isVar == true, field.type)
    }

fun JsStatement.withJsDocForProperty(property: IrProperty, context: JsGenerationContext) =
    annotateWithContext(context) {
        val type = property.backingField?.type ?: property.getter?.returnType ?: return@annotateWithContext

        if (property.isExternal) {
            public()
        } else {
            export()
        }

        mutability(isMutable = property.isVar, type)
    }

fun JsStatement.withJsDocForFunction(function: IrSimpleFunction, context: JsGenerationContext) =
    annotateWithContext(context) {
        if (function.parentClassOrNull != null) {
            exportability(
                function.correspondingPropertySymbol == null &&
                        function.isExported(context.staticContext.backendContext) &&
                        !function.isMangled(context)
            )
            visibility(function.visibility)
            inheritable(function.isOverridableMemberOrAccessor())
        }

        withParams(function.valueParameters)
        withReturnType(function.returnType)
        withTypeVariables(function.typeParameters)
    }

fun JsStatement.withJsDocForConstructor(constructor: IrFunction, context: JsGenerationContext) =
    withJsDocForConstructor(constructor, constructor.parentAsClass, context)

fun JsStatement.withJsDocForConstructor(constructor: IrFunction?, parentClass: IrClass, context: JsGenerationContext) =
    annotateWithContext(context) {
        val baseClass = parentClass.superTypes.firstOrNull { !it.classifierOrFail.isInterface }
        val interfaces = parentClass.superTypes.filter { it !== baseClass && !it.isAny() }
        if (parentClass.isInterface) {
            if (parentClass.isEffectivelyExternal()) {
                record()
            } else {
                interface_()
            }
            extends(interfaces)
        } else {
            constructor()
            inheritable(!parentClass.isFinalClass)
            extends(baseClass?.let { listOf(it) }.orEmpty())
            implements(interfaces)
            constructor?.let { withParams(it.valueParameters) }
        }
    }
