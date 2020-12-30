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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

interface IrSymbolOwner : IrElement {
    val symbol: IrSymbol
}

interface IrMetadataSourceOwner : IrElement {
    var metadata: MetadataSource?
}

interface IrDeclaration : IrStatement, IrSymbolOwner, IrMutableAnnotationContainer {
    @ObsoleteDescriptorBasedAPI
    val descriptor: DeclarationDescriptor

    var origin: IrDeclarationOrigin

    var parent: IrDeclarationParent

    val factory: IrFactory
}

abstract class IrDeclarationBase : IrElementBase(), IrDeclaration

interface IrOverridableDeclaration<S : IrSymbol> : IrDeclaration {
    var overriddenSymbols: List<S>
}

interface IrDeclarationWithVisibility : IrDeclaration {
    var visibility: DescriptorVisibility
}

interface IrDeclarationWithName : IrDeclaration {
    val name: Name
}

interface IrOverridableMember : IrDeclarationWithVisibility, IrDeclarationWithName, IrSymbolOwner {
    val modality: Modality
}

interface IrMemberWithContainerSource : IrDeclarationWithName {
    val containerSource: DeserializedContainerSource?
}
