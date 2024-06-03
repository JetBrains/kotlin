/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirReturn
import org.jetbrains.kotlin.bir.symbols.BirReturnTargetSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.ForwardReferenceRecorder

class BirReturnImpl(
    sourceSpan: SourceSpan,
    type: BirType,
    value: BirExpression,
    returnTargetSymbol: BirReturnTargetSymbol,
) : BirReturn() {
    override var sourceSpan: SourceSpan = sourceSpan

    override var attributeOwnerId: BirAttributeContainer = this

    override var type: BirType = type

    private var _value: BirExpression? = value
    override var value: BirExpression
        get() {
            return _value ?: throwChildElementRemoved("value")
        }
        set(value) {
            if (_value !== value) {
                childReplaced(_value, value)
                _value = value
            }
        }

    private var _returnTargetSymbol: BirReturnTargetSymbol = returnTargetSymbol
    override var returnTargetSymbol: BirReturnTargetSymbol
        get() {
            return _returnTargetSymbol
        }
        set(value) {
            if (_returnTargetSymbol !== value) {
                _returnTargetSymbol = value
                forwardReferencePropertyChanged()
            }
        }


    init {
        initChild(_value)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _value?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._value === old -> {
                this._value = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getForwardReferences(recorder: ForwardReferenceRecorder) {
        recorder.recordReference(returnTargetSymbol)
    }
}
