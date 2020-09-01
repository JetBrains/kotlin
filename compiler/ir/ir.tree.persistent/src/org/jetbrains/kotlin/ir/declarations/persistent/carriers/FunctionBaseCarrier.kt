/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.IrType

internal interface FunctionBaseCarrier : DeclarationCarrier {
    var returnTypeFieldField: IrType
    var dispatchReceiverParameterField: IrValueParameter?
    var extensionReceiverParameterField: IrValueParameter?
    var bodyField: IrBody?
    var metadataField: MetadataSource?
    var visibilityField: DescriptorVisibility
    var typeParametersField: List<IrTypeParameter>
    var valueParametersField: List<IrValueParameter>
}
