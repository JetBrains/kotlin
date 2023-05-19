/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import java.util.concurrent.atomic.AtomicBoolean

@DslMarker
internal annotation class StateKeeperDsl

internal typealias PostProcessor = () -> Unit

@StateKeeperDsl
internal interface StateKeeperBuilder {
    fun register(state: PreservedState)
}

@JvmInline
@StateKeeperDsl
internal value class StateKeeperScope<Owner : Any>(private val owner: Owner) {
    context(StateKeeperBuilder)
    inline fun <Value> add(provider: (Owner) -> Value, crossinline mutator: (Owner, Value) -> Unit, arranger: (Value & Any) -> Value) {
        val owner = this@StateKeeperScope.owner

        val storedValue = provider(owner)
        if (storedValue != null) {
            val arrangedValue = arranger(storedValue)
            if (arrangedValue !== storedValue) {
                mutator(owner, arrangedValue)
            }
        }

        register(object : PreservedState {
            override fun restore() = mutator(owner, storedValue)
            override fun postProcess() {}
        })
    }

    context(StateKeeperBuilder)
    inline fun <Value> add(provider: (Owner) -> Value, crossinline mutator: (Owner, Value) -> Unit) {
        val owner = this@StateKeeperScope.owner
        val storedValue = provider(owner)

        register(object : PreservedState {
            override fun restore() = mutator(owner, storedValue)
            override fun postProcess() {}
        })
    }

    context(StateKeeperBuilder)
    fun add(keeper: StateKeeper<Owner>) {
        val owner = this@StateKeeperScope.owner
        register(keeper.prepare(owner))
    }

    context(StateKeeperBuilder)
    fun postProcess(block: PostProcessor) {
        register(object : PreservedState {
            override fun postProcess() = block()
            override fun restore() {}
        })
    }
}

context(StateKeeperBuilder)
internal fun <Entity : Any> entity(entity: Entity?, keeper: StateKeeper<Entity>) {
    if (entity != null) {
        StateKeeperScope(entity).add(keeper)
    }
}

context(StateKeeperBuilder)
internal inline fun <Entity : Any> entity(entity: Entity?, block: StateKeeperScope<Entity>.(Entity) -> Unit) {
    if (entity != null) {
        StateKeeperScope(entity).block(entity)
    }
}

context(StateKeeperBuilder)
internal inline fun <Entity : Any> entityList(list: List<Entity?>?, block: StateKeeperScope<Entity>.(Entity) -> Unit) {
    if (list != null) {
        for (entity in list) {
            if (entity != null) {
                StateKeeperScope(entity).block(entity)
            }
        }
    }
}

internal fun <Owner : Any> stateKeeper(block: context(StateKeeperBuilder) StateKeeperScope<Owner>.(Owner) -> Unit): StateKeeper<Owner> {
    return StateKeeper { owner ->
        val states = mutableListOf<PreservedState>()

        val builder = object : StateKeeperBuilder {
            override fun register(state: PreservedState) {
                states += state
            }
        }

        val scope = StateKeeperScope(owner)
        block(builder, scope, owner)

        object : PreservedState {
            private val postProcessed = AtomicBoolean(false)
            private val isRestored = AtomicBoolean(false)

            override fun postProcess() {
                if (!postProcessed.compareAndSet(false, true)) return
                states.forEach { it.postProcess() }
            }

            override fun restore() {
                if (!isRestored.compareAndSet(false, true)) return
                states.forEach { it.restore() }
            }
        }
    }
}

internal class StateKeeper<in Owner : Any>(val provider: (Owner) -> PreservedState) {
    fun prepare(owner: Owner): PreservedState {
        return provider(owner)
    }
}

internal inline fun <Target : FirElementWithResolveState, Result> resolveWithKeeper(
    target: Target,
    keeper: StateKeeper<Target>,
    prepareTarget: (Target) -> Unit,
    action: () -> Result
): Result {
    if (target.moduleData.session.isOnAirAnalysis) {
        // Several arrangers reset declaration bodies to lazy ones, and then re-construct the FIR tree from the backing PSI.
        // This won't work for on-air analysis, as the tree is manually modified. However, it doesn't seem that tree guards are needed
        // in the first place, as results of the on-air analysis aren't going to be shared.
        return action()
    }

    var preservedState: PreservedState? = null
    var isSuccessful = false

    try {
        preservedState = keeper.prepare(target)
        prepareTarget(target)
        preservedState.postProcess()
        val result = action()
        isSuccessful = true
        return result
    } catch (e: Throwable) {
        throw e
    } finally {
        if (!isSuccessful) {
            preservedState?.restore()
        }
    }
}

internal interface PreservedState {
    fun postProcess()
    fun restore()
}