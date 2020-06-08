/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

interface IrProperty :
    IrDeclarationWithName,
    IrDeclarationWithVisibility,
    IrOverridableMember,
    IrSymbolOwner {

    @DescriptorBasedIr
    override val descriptor: PropertyDescriptor
    override val symbol: IrPropertySymbol

    val isVar: Boolean
    val isConst: Boolean
    val isLateinit: Boolean
    val isDelegated: Boolean
    val isExternal: Boolean
    val isExpect: Boolean
    val isFakeOverride: Boolean

    var backingField: IrField?
    var getter: IrSimpleFunction?
    var setter: IrSimpleFunction?
}