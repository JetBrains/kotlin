/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import java.util.concurrent.atomic.AtomicBoolean

internal interface StateKeeperBuilder<Owner> {
    fun <Value> add(provider: (Owner) -> Value, mutator: (Owner, Value) -> Unit, arranger: ((Owner) -> Value)? = null)
}

internal fun <Item : Any, Value> StateKeeperBuilder<*>.addItem(
    item: Item?,
    provider: (Item) -> Value,
    mutator: (Item, Value) -> Unit,
    arranger: ((Item) -> Value)? = null
) {
    if (item != null) {
        add(
            provider = { provider(item) },
            mutator = { _, value -> mutator(item, value) },
            arranger = if (arranger != null) { _ -> arranger(item) } else null
        )
    }
}

internal fun <Item : Any, Value> StateKeeperBuilder<*>.addList(
    items: List<Item?>?,
    provider: (Item) -> Value,
    mutator: (Item, Value) -> Unit,
    arranger: ((Item) -> Value)? = null
) {
    if (items != null) {
        for (item in items) {
            addItem(item, provider, mutator, arranger)
        }
    }
}

internal fun <Item : Any> StateKeeperBuilder<*>.stateItem(item: Item?, block: StateKeeperBuilder<Item>.(Item) -> Unit) {
    if (item != null) {
        CustomStateKeeperBuilder(this, item).block(item)
    }
}

internal fun <Item : Any> StateKeeperBuilder<*>.stateList(list: List<Item>?, block: StateKeeperBuilder<Item>.(Item) -> Unit) {
    if (list != null) {
        for (item in list) {
            CustomStateKeeperBuilder(this, item).block(item)
        }
    }
}

private class CustomStateKeeperBuilder<Item>(private val parent: StateKeeperBuilder<*>, private val item: Item) : StateKeeperBuilder<Item> {
    override fun <Value> add(provider: (Item) -> Value, mutator: (Item, Value) -> Unit, arranger: ((Item) -> Value)?) {
        parent.addItem(item, provider, mutator, arranger)
    }
}

internal fun <Owner : Any> stateKeeper(block: StateKeeperBuilder<Owner>.(Owner) -> Unit): StateKeeper<Owner> {
    return stateKeeper(null, block)
}

internal fun <Delegate : Any, Owner : Delegate> stateKeeper(
    delegate: StateKeeper<Delegate>?,
    block: StateKeeperBuilder<Owner>.(Owner) -> Unit
): StateKeeper<Owner> {
    return StateKeeper { owner ->
        val state = mutableListOf<PreservedState>()

        if (delegate != null) {
            state += delegate.prepare(owner)
        }

        val builder = object : StateKeeperBuilder<Owner> {
            override fun <Value> add(provider: (Owner) -> Value, mutator: (Owner, Value) -> Unit, arranger: ((Owner) -> Value)?) {
                val storedValue = provider(owner)
                if (storedValue != null && arranger != null) {
                    mutator(owner, arranger.invoke(owner))
                }

                state += PreservedState { mutator(owner, storedValue) }
            }
        }

        builder.block(owner)
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