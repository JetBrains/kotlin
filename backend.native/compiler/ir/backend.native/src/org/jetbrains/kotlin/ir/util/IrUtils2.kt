/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType

//TODO: delete file on next kotlin dependency update
internal fun IrExpression.isNullConst() = this is IrConst<*> && this.kind == IrConstKind.Null

private var topLevelInitializersCounter = 0

internal fun IrFile.addTopLevelInitializer(expression: IrExpression) {
    val fieldDescriptor = PropertyDescriptorImpl.create(
            this.packageFragmentDescriptor,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PRIVATE,
            false,
            "topLevelInitializer${topLevelInitializersCounter++}".synthesizedName,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false,
            false,
            false,
            false,
            false,
            false
    )

    val builtIns = fieldDescriptor.builtIns
    fieldDescriptor.setType(builtIns.unitType, emptyList(), null, null as KotlinType?)
    fieldDescriptor.initialize(null, null)

    val irField = IrFieldImpl(
            expression.startOffset, expression.endOffset,
            IrDeclarationOrigin.DEFINED, fieldDescriptor
    )

    val initializer = IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset, builtIns.unitType,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, builtIns.unitType, expression
    )

    irField.initializer = IrExpressionBodyImpl(expression.startOffset, expression.endOffset, initializer)

    this.declarations.add(irField)
}
