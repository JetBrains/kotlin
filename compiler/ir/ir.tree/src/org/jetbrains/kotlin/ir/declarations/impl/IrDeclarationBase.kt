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

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.carriers.BodyCarrier
import org.jetbrains.kotlin.ir.declarations.impl.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.impl.carriers.DeclarationCarrier
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

abstract class IrDeclarationBase<T : DeclarationCarrier<T>>(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin
) : IrPersistingElementBase<T>(startOffset, endOffset),
    IrDeclaration,
    DeclarationCarrier<T> {

    override var parentField: IrDeclarationParent? = null

    override var parent: IrDeclarationParent
        get() = getCarrier().parentField ?: throw UninitializedPropertyAccessException("Parent not initialized: $this")
        set(p) {
            if (getCarrier().parentField !== p) {
                setCarrier().parentField = p
            }
        }

    override var originField: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
        get() = getCarrier().originField
        set(p) {
            if (getCarrier().originField !== p) {
                setCarrier().originField = p
            }
        }

    var removedOn: Int = Int.MAX_VALUE

    override var annotationsField: List<IrConstructorCall> = emptyList()

    override var annotations: List<IrConstructorCall>
        get() = getCarrier().annotationsField
        set(v) {
            if (getCarrier().annotationsField !== v) {
                setCarrier().annotationsField = v
            }
        }

    override val metadata: MetadataSource?
        get() = null

    override fun ensureLowered() {
        if (stageController.currentStage > loweredUpTo) {
            stageController.lazyLower(this)
        }
    }
}

abstract class IrPersistingElementBase<T : Carrier<T>>(
    startOffset: Int,
    endOffset: Int
) : IrElementBase(startOffset, endOffset),
    Carrier<T> {

    override var lastModified: Int = stageController.currentStage

    var loweredUpTo = stageController.currentStage

    private var values: Array<Any?>? = null

    val createdOn: Int = stageController.currentStage
//        get() = values?.let { (it[0] as T).lastModified } ?: lastModified

    abstract fun ensureLowered()

    protected fun getCarrier(): T {
        stageController.currentStage.let { stage ->
            ensureLowered()

            if (stage >= lastModified) return this as T

            if (stage < createdOn) error("Access before creation")

            val v = values
                ?: error("How come?")

            var l = -1
            var r = v.size
            while (r - l > 1) {
                val m = (l + r) / 2
                if ((v[m] as T).lastModified <= stage) {
                    l = m
                } else {
                    r = m
                }
            }
            if (l < 0) {
                error("access before creation")
            }

            return v[l] as T
        }
    }

    protected fun setCarrier(): T {
        val stage = stageController.currentStage

        ensureLowered()

        if (!stageController.canModify(this)) {
            error("Cannot modify this element!")
        }

        if (loweredUpTo > stage) {
            error("retrospective modification")
        }

        if (stage == lastModified) {
            return this as T
        } else {
            val newValues = values?.let { oldValues ->
                oldValues.copyOf(oldValues.size + 1)
            } ?: arrayOfNulls<Any?>(1)

            newValues[newValues.size - 1] = this.clone()

            values = newValues
        }

        this.lastModified = stage

        return this as T
    }
}

abstract class IrBodyBase<B : IrBodyBase<B>>(
    startOffset: Int,
    endOffset: Int,
    private var initializer: (B.() -> Unit)?
) : IrPersistingElementBase<BodyCarrier>(startOffset, endOffset), IrBody, BodyCarrier {
    override var containerField: IrDeclaration? = null

    var container: IrDeclaration
        get() = getCarrier().containerField!!
        set(p) {
            if (getCarrier().containerField !== p) {
                setCarrier().containerField = p
            }
        }

    protected fun <T> checkEnabled(fn: () -> T): T {
        if (!stageController.bodiesEnabled) error("Bodies disabled!")
        ensureLowered()
        return fn()
    }

    override fun ensureLowered() {
        initializer?.let { initFn ->
            initializer = null
            stageController.withStage(createdOn) {
                stageController.bodyLowering {
                    initFn.invoke(this as B)
                }
            }
        }
        if (loweredUpTo + 1 < stageController.currentStage) {
            stageController.lazyLower(this)
        }
    }
}