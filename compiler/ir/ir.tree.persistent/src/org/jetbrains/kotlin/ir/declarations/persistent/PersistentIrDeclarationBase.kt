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

package org.jetbrains.kotlin.ir.declarations.persistent

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.BodyCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.DeclarationCarrier
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

interface PersistentIrDeclarationBase<T : DeclarationCarrier> : PersistentIrElementBase<T>, IrDeclaration, DeclarationCarrier {
    var removedOn: Int

    override val factory: IrFactory
        get() = PersistentIrFactory

    // TODO reduce boilerplate
    override var parent: IrDeclarationParent
        get() = getCarrier().parentField ?: throw UninitializedPropertyAccessException("Parent not initialized: $this")
        set(p) {
            if (getCarrier().parentField !== p) {
                setCarrier().parentField = p
            }
        }

    override var origin: IrDeclarationOrigin
        get() = getCarrier().originField
        set(p) {
            if (getCarrier().originField !== p) {
                setCarrier().originField = p
            }
        }

    override var annotations: List<IrConstructorCall>
        get() = getCarrier().annotationsField
        set(v) {
            if (getCarrier().annotationsField !== v) {
                setCarrier().annotationsField = v
            }
        }

    override fun ensureLowered() {
        if (stageController.currentStage > loweredUpTo) {
            stageController.lazyLower(this)
        }
    }
}

interface PersistentIrElementBase<T : Carrier> : IrElement, Carrier {
    override var lastModified: Int

    var loweredUpTo: Int

    // TODO Array<T>?
    var values: Array<Carrier>?

    val createdOn: Int

    fun ensureLowered()

    @Suppress("UNCHECKED_CAST")
    fun getCarrier(): T {
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

    // TODO naming? e.g. `mutableCarrier`
    @Suppress("UNCHECKED_CAST")
    fun setCarrier(): T {
        val stage = stageController.currentStage

        ensureLowered()

        if (!stageController.canModify(this)) {
            error("Cannot modify this element!")
        }

        if (loweredUpTo > stage) {
            error("retrospective modification")
        }

        // TODO move up? i.e. fast path
        if (stage == lastModified) {
            return this as T
        } else {
            values = (values ?: emptyArray()) + this.clone() as T
        }

        this.lastModified = stage

        return this as T
    }
}

interface PersistentIrBodyBase<B : PersistentIrBodyBase<B>> : PersistentIrElementBase<BodyCarrier>, BodyCarrier {
    var initializer: (B.() -> Unit)?

    override var containerField: IrDeclaration?

    var container: IrDeclaration
        get() = getCarrier().containerField!!
        set(p) {
            if (getCarrier().containerField !== p) {
                setCarrier().containerField = p
            }
        }

    fun <T> checkEnabled(fn: () -> T): T {
        if (!stageController.bodiesEnabled) error("Bodies disabled!")
        ensureLowered()
        return fn()
    }

    @Suppress("UNCHECKED_CAST")
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
            stageController.lazyLower(this as IrBody)
        }
    }
}
