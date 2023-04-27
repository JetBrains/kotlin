/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import java.util.concurrent.atomic.AtomicBoolean

@DslMarker
internal annotation class StateKeeperDsl

@StateKeeperDsl
internal interface StateKeeperBuilder {
    fun register(restorer: () -> Unit)
}

@JvmInline
@StateKeeperDsl
internal value class StateKeeperScope<Owner : Any>(private val owner: Owner) {
    context(StateKeeperBuilder)
    inline fun <Value> add(provider: (Owner) -> Value, crossinline mutator: (Owner, Value) -> Unit, arranger: (Owner) -> Value) {
        val owner = this@StateKeeperScope.owner

        val storedValue = provider(owner)
        if (storedValue != null) {
            mutator(owner, arranger(owner))
        }

        register { mutator(owner, storedValue) }
    }

    context(StateKeeperBuilder)
    inline fun <Value> add(provider: (Owner) -> Value, crossinline mutator: (Owner, Value) -> Unit) {
        val owner = this@StateKeeperScope.owner
        val storedValue = provider(owner)
        register { mutator(owner, storedValue) }
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

internal inline fun <Owner : Any> stateKeeper(
    crossinline block: context(StateKeeperBuilder) StateKeeperScope<Owner>.(Owner) -> Unit
): StateKeeper<Owner> {
    return stateKeeper(null, block)
}

internal inline fun <Delegate : Any, Owner : Delegate> stateKeeper(
    delegate: StateKeeper<Delegate>?,
    crossinline block: context(StateKeeperBuilder) StateKeeperScope<Owner>.(Owner) -> Unit
): StateKeeper<Owner> {
    return StateKeeper { owner ->
        val state = mutableListOf<PreservedState>()

        if (delegate != null) {
            state += delegate.prepare(owner)
        }

        val builder = object : StateKeeperBuilder {
            override fun register(restorer: () -> Unit) {
                state += PreservedState(restorer)
            }
        }

        val scope = StateKeeperScope(owner)
        block(builder, scope, owner)

        return@StateKeeper state
    }
}

internal class StateKeeper<in Owner : Any>(val provider: (Owner) -> List<PreservedState>) {
    fun prepare(owner: Owner): PreservedState {
        val states = provider(owner)

        return object : PreservedState {
            private val isRestored = AtomicBoolean(false)

            override fun restore() {
                if (!isRestored.compareAndSet(false, true)) return
                states.forEach { it.restore() }
            }
        }
    }
}

internal fun <Target : Any, Result> resolve(target: Target, keeper: StateKeeper<Target>, block: () -> Result): Result {
    var preservedState: PreservedState? = null

    var isSuccessful = false
    try {
        preservedState = keeper.prepare(target)
        val result = block()
        isSuccessful = true
        return result
    } finally {
        if (!isSuccessful) {
            preservedState?.restore()
        }
    }
}

internal fun interface PreservedState {
    companion object Empty : PreservedState {
        override fun restore() {}
    }

    fun restore()
}