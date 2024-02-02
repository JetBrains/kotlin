/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirConstructor
import org.jetbrains.kotlin.bir.declarations.BirScript
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirScriptSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirScriptImpl(
    sourceSpan: CompressedSourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    thisReceiver: BirValueParameter?,
    baseClass: BirType?,
    providedProperties: List<BirPropertySymbol>,
    resultProperty: BirPropertySymbol?,
    earlierScriptsParameter: BirValueParameter?,
    importedScripts: List<BirScriptSymbol>?,
    earlierScripts: List<BirScriptSymbol>?,
    targetClass: BirClassSymbol?,
    constructor: BirConstructor?,
) : BirScript(BirScript) {
    override val owner: BirScriptImpl
        get() = this

    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan = sourceSpan

    override var signature: IdSignature? = signature

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    private var _thisReceiver: BirValueParameter? = thisReceiver
    override var thisReceiver: BirValueParameter?
        get() {
            return _thisReceiver
        }
        set(value) {
            if (_thisReceiver !== value) {
                childReplaced(_thisReceiver, value)
                _thisReceiver = value
            }
        }

    override var baseClass: BirType? = baseClass

    override var providedProperties: List<BirPropertySymbol> = providedProperties

    override var resultProperty: BirPropertySymbol? = resultProperty

    private var _earlierScriptsParameter: BirValueParameter? = earlierScriptsParameter
    override var earlierScriptsParameter: BirValueParameter?
        get() {
            return _earlierScriptsParameter
        }
        set(value) {
            if (_earlierScriptsParameter !== value) {
                childReplaced(_earlierScriptsParameter, value)
                _earlierScriptsParameter = value
            }
        }

    override var importedScripts: List<BirScriptSymbol>? = importedScripts

    override var earlierScripts: List<BirScriptSymbol>? = earlierScripts

    override var targetClass: BirClassSymbol? = targetClass

    override var constructor: BirConstructor? = constructor

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)
    override val statements: BirImplChildElementList<BirStatement> = BirImplChildElementList(this, 2, false)
    override val explicitCallParameters: BirImplChildElementList<BirVariable> = BirImplChildElementList(this, 3, false)
    override val implicitReceiversParameters: BirImplChildElementList<BirValueParameter> = BirImplChildElementList(this, 4, false)
    override val providedPropertiesParameters: BirImplChildElementList<BirValueParameter> = BirImplChildElementList(this, 5, false)

    init {
        initChild(_thisReceiver)
        initChild(_earlierScriptsParameter)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        statements.acceptChildrenLite(visitor)
        _thisReceiver?.acceptLite(visitor)
        explicitCallParameters.acceptChildrenLite(visitor)
        implicitReceiversParameters.acceptChildrenLite(visitor)
        providedPropertiesParameters.acceptChildrenLite(visitor)
        _earlierScriptsParameter?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._thisReceiver === old -> {
                this._thisReceiver = new as BirValueParameter?
            }
            this._earlierScriptsParameter === old -> {
                this._earlierScriptsParameter = new as BirValueParameter?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.annotations
            2 -> this.statements
            3 -> this.explicitCallParameters
            4 -> this.implicitReceiversParameters
            5 -> this.providedPropertiesParameters
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
