/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirClassImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    override var isExternal: Boolean,
    override var visibility: DescriptorVisibility,
    override var originalBeforeInline: BirAttributeContainer?,
    override var metadata: MetadataSource?,
    override var kind: ClassKind,
    override var modality: Modality,
    override var isCompanion: Boolean,
    override var isInner: Boolean,
    override var isData: Boolean,
    override var isValue: Boolean,
    override var isExpect: Boolean,
    override var isFun: Boolean,
    override val source: SourceElement,
    override var superTypes: List<BirType>,
    thisReceiver: BirValueParameter?,
    override var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?,
    override var sealedSubclasses: List<BirClassSymbol>,
) : BirClass() {
    override var typeParameters: BirChildElementList<BirTypeParameter> =
            BirChildElementList(this)

    override val declarations: BirChildElementList<BirDeclaration> =
            BirChildElementList(this)

    override var attributeOwnerId: BirAttributeContainer = this

    private var _thisReceiver: BirValueParameter? = thisReceiver

    override var thisReceiver: BirValueParameter?
        get() = _thisReceiver
        set(value) {
            if (_thisReceiver != value) {
                replaceChild(_thisReceiver, value)
                _thisReceiver = value
            }
        }
    init {
        initChild(_thisReceiver)
    }
}
