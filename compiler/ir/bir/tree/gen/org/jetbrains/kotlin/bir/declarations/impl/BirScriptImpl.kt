/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirConstructor
import org.jetbrains.kotlin.bir.declarations.BirScript
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirScriptSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirScriptImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: DeclarationDescriptor,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var name: Name,
    override var metadata: MetadataSource?,
    thisReceiver: BirValueParameter?,
    override var baseClass: BirType?,
    override var providedProperties: List<BirPropertySymbol>,
    override var resultProperty: BirPropertySymbol?,
    earlierScriptsParameter: BirValueParameter?,
    override var importedScripts: List<BirScriptSymbol>?,
    override var earlierScripts: List<BirScriptSymbol>?,
    override var targetClass: BirClassSymbol?,
    override var constructor: BirConstructor?,
) : BirScript() {
    override val statements: BirChildElementList<BirStatement> = BirChildElementList(this, 0)

    private var _thisReceiver: BirValueParameter? = thisReceiver

    override var thisReceiver: BirValueParameter?
        get() = _thisReceiver
        set(value) {
            if (_thisReceiver != value) {
                replaceChild(_thisReceiver, value)
                _thisReceiver = value
            }
        }

    override var explicitCallParameters: BirChildElementList<BirVariable> =
            BirChildElementList(this, 1)

    override var implicitReceiversParameters: BirChildElementList<BirValueParameter> =
            BirChildElementList(this, 2)

    override var providedPropertiesParameters: BirChildElementList<BirValueParameter> =
            BirChildElementList(this, 3)

    private var _earlierScriptsParameter: BirValueParameter? = earlierScriptsParameter

    override var earlierScriptsParameter: BirValueParameter?
        get() = _earlierScriptsParameter
        set(value) {
            if (_earlierScriptsParameter != value) {
                replaceChild(_earlierScriptsParameter, value)
                _earlierScriptsParameter = value
            }
        }
    init {
        initChild(_thisReceiver)
        initChild(_earlierScriptsParameter)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._thisReceiver === old -> this.thisReceiver = new as BirValueParameter
            this._earlierScriptsParameter === old -> this.earlierScriptsParameter = new as
                BirValueParameter
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when {
        id == 0 -> this.statements
        id == 1 -> this.explicitCallParameters
        id == 2 -> this.implicitReceiversParameters
        id == 3 -> this.providedPropertiesParameters
        else -> throwChildrenListWithIdNotFound(id)
    }
}
