/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

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
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
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
) : BirScript(), BirScriptSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        providedProperties: List<BirPropertySymbol>,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        thisReceiver = null,
        baseClass = null,
        providedProperties = providedProperties,
        resultProperty = null,
        earlierScriptsParameter = null,
        importedScripts = null,
        earlierScripts = null,
        targetClass = null,
        constructor = null,
    )

    override val owner: BirScriptImpl
        get() = this

    override val isBound: Boolean
        get() = true

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var annotations: List<BirConstructorCall> = annotations

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override val symbol: BirScriptSymbol
        get() = this

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

    override val statements: BirImplChildElementList<BirStatement> = BirImplChildElementList(this, 1, false)

    override val explicitCallParameters: BirImplChildElementList<BirVariable> = BirImplChildElementList(this, 2, false)

    override val implicitReceiversParameters: BirImplChildElementList<BirValueParameter> = BirImplChildElementList(this, 3, false)

    override val providedPropertiesParameters: BirImplChildElementList<BirValueParameter> = BirImplChildElementList(this, 4, false)


    init {
        initChild(_thisReceiver)
        initChild(_earlierScriptsParameter)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
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
            1 -> this.statements
            2 -> this.explicitCallParameters
            3 -> this.implicitReceiversParameters
            4 -> this.providedPropertiesParameters
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
