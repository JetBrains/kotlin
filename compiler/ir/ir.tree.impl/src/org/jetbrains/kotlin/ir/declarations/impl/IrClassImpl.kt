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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrClassImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    override val name: Name,
    override val kind: ClassKind,
    override var visibility: DescriptorVisibility,
    modality: Modality,
    isCompanion: Boolean = false,
    isInner: Boolean = false,
    isData: Boolean = false,
    isExternal: Boolean = false,
    isInline: Boolean = false,
    isExpect: Boolean = false,
    isFun: Boolean = false,
    override val source: SourceElement = SourceElement.NO_SOURCE
) : IrClass() {
    init {
        symbol.bind(this)
    }

    private var flags: Int = modality.toFlags() or
            isCompanion.toFlag(IrFlags.IS_COMPANION) or isInner.toFlag(IrFlags.IS_INNER) or isData.toFlag(IrFlags.IS_DATA) or
            isExternal.toFlag(IrFlags.IS_EXTERNAL) or isInline.toFlag(IrFlags.IS_INLINE) or isExpect.toFlag(IrFlags.IS_EXPECT) or
            isFun.toFlag(IrFlags.IS_FUN)

    override val factory: IrFactory
        get() = IrFactoryImpl

    override var modality: Modality
        get() = flags.toModality()
        set(value) {
            flags = flags.setModality(value)
        }

    override val isCompanion: Boolean
        get() = flags.getFlag(IrFlags.IS_COMPANION)

    override val isInner: Boolean
        get() = flags.getFlag(IrFlags.IS_INNER)

    override val isData: Boolean
        get() = flags.getFlag(IrFlags.IS_DATA)

    override val isExternal: Boolean
        get() = flags.getFlag(IrFlags.IS_EXTERNAL)

    override val isInline: Boolean
        get() = flags.getFlag(IrFlags.IS_INLINE)

    override val isExpect: Boolean
        get() = flags.getFlag(IrFlags.IS_EXPECT)

    override val isFun: Boolean
        get() = flags.getFlag(IrFlags.IS_FUN)

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override var thisReceiver: IrValueParameter? = null

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var superTypes: List<IrType> = emptyList()

    override var metadata: MetadataSource? = null

    override var attributeOwnerId: IrAttributeContainer = this
}
