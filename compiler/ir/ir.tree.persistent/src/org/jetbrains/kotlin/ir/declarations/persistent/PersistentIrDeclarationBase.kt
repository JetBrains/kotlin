/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.ir.declarations.persistent

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.BodyCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.DeclarationCarrier
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface PersistentIrDeclarationBase<T : DeclarationCarrier> : PersistentIrElementBase<T>, IrDeclaration, DeclarationCarrier {
    var removedOn: Int


    override var parentField: IrDeclarationParent?

    override var parentSymbolField: IrSymbol?
        get() = parentField?.let { (it as IrSymbolOwner).symbol }
        set(v) {
            parentField = v?.owner?.cast()
        }

    override var originField: IrDeclarationOrigin

    override var annotationsField: List<IrConstructorCall>

    var signature: IdSignature?

    // TODO reduce boilerplate
    override var parent: IrDeclarationParent
        get() = getCarrier().parentField ?: throw UninitializedPropertyAccessException("Parent not initialized: $this")
        set(p) {
            if (getCarrier().parentField !== p) {
                setCarrier()
                parentField = p
            }
        }

    override var origin: IrDeclarationOrigin
        get() = getCarrier().originField
        set(p) {
            if (getCarrier().originField !== p) {
                setCarrier()
                originField = p
            }
        }

    override var annotations: List<IrConstructorCall>
        get() = getCarrier().annotationsField
        set(v) {
            if (getCarrier().annotationsField !== v) {
                setCarrier()
                annotationsField = v
            }
        }

    override fun ensureLowered() {
        if (factory.stageController.currentStage > loweredUpTo) {
            factory.stageController.lazyLower(this)
        }
    }
}

interface PersistentIrElementBase<T : Carrier> : IrElement, Carrier {

    val factory: PersistentIrFactory

    override var lastModified: Int

    var loweredUpTo: Int

    // TODO Array<T>?
    var values: Array<Carrier>?

    val createdOn: Int

    fun ensureLowered()

    @Suppress("UNCHECKED_CAST")
    fun getCarrier(): T {
        factory.stageController.currentStage.let { stage ->
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
    fun setCarrier() {
        val stage = factory.stageController.currentStage

        ensureLowered()

        if (!factory.stageController.canModify(this)) {
            error("Cannot modify this element!")
        }

        if (loweredUpTo > stage) {
            error("retrospective modification")
        }

        // TODO move up? i.e. fast path
        if (stage == lastModified) {
            return
        } else {
            values = (values ?: emptyArray()) + this.clone() as T
        }

        this.lastModified = stage
    }
}

interface PersistentIrBodyBase<B : PersistentIrBodyBase<B>> : PersistentIrElementBase<BodyCarrier>, BodyCarrier {
    var initializer: (B.() -> Unit)?

    override var containerField: IrDeclaration?

    override var containerFieldSymbol: IrSymbol?
        get() = (containerField as? IrSymbolOwner)?.symbol
        set(s) {
            containerField = s?.owner?.cast()
        }

    val hasContainer: Boolean
        get() = getCarrier().containerField != null

    var container: IrDeclaration
        get() = getCarrier().containerField!!
        set(p) {
            if (getCarrier().containerField !== p) {
                setCarrier()
                containerField = p
            }
        }

    fun <T> checkEnabled(fn: () -> T): T {
        if (!factory.stageController.bodiesEnabled) error("Bodies disabled!")
        ensureLowered()
        return fn()
    }

    @Suppress("UNCHECKED_CAST")
    override fun ensureLowered() {
        initializer?.let { initFn ->
            initializer = null
            factory.stageController.withStage(createdOn) {
                factory.stageController.bodyLowering {
                    initFn.invoke(this as B)
                }
            }
        }
        if (loweredUpTo + 1 < factory.stageController.currentStage) {
            factory.stageController.lazyLower(this as IrBody)
        }
    }
}
