/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import java.util.concurrent.atomic.AtomicBoolean

internal fun <Delegate : Any, Owner : Delegate> stateKeeper(
    delegate: StateKeeper<Delegate>,
    block: StateKeeperBuilder<Delegate, Owner>.() -> Unit
): StateKeeper<Owner> {
    val builder = StateKeeperBuilder<Delegate, Owner>(delegate)
    block(builder)
    return builder.build()
}

internal fun <Owner : Any> stateKeeper(block: StateKeeperBuilder<Owner, Owner>.() -> Unit): StateKeeper<Owner> {
    val builder = StateKeeperBuilder<Owner, Owner>(null)
    block(builder)
    return builder.build()
}

internal fun interface StateItemBuilder<Owner : Any> {
    fun add(item: PreservedStateItem<Owner, *, *>)
}

internal fun <Owner : Any, Value> StateItemBuilder<Owner>.add(
    provider: (Owner) -> Value,
    mutator: (Owner, Value) -> Unit,
    arranger: ((Owner) -> Value)? = null
) {
    addNested({ it }, provider, mutator, arranger)
}

internal fun <Owner : Any, Node : Any, Value> StateItemBuilder<Owner>.addNested(
    mapper: (Owner) -> Node?,
    provider: (Node) -> Value,
    mutator: (Node, Value) -> Unit,
    arranger: ((Node) -> Value)? = null
) {
    add(PreservedStateItem(mapper, provider, mutator, arranger))
}

internal class PreservedStateItem<in Owner : Any, Node : Any, Value>(
    private val mapper: (Owner) -> Node?,
    private val provider: (Node) -> Value,
    private val mutator: (Node, Value) -> Unit,
    private val arranger: ((Node) -> Value)?
) {
    fun prepare(owner: Owner): PreservedState {
        val node = mapper(owner) ?: return PreservedState.Empty
        val storedValue = provider(node)

        if (storedValue != null && arranger != null) {
            mutator(node, arranger.invoke(node))
        }

        return object : PreservedState {
            override fun restore() {
                mutator(node, storedValue)
            }
        }
    }
}

internal class StateKeeperBuilder<Delegate : Any, Owner : Delegate>(delegate: StateKeeper<Delegate>?) : StateItemBuilder<Owner> {
    private val providers = mutableListOf<PreservedStateItemProvider<Owner>>()

    init {
        if (delegate != null) {
            providers.addAll(delegate.providers)
        }
    }

    override fun add(item: PreservedStateItem<Owner, *, *>) {
        providers += PreservedStateItemProvider { listOf(item) }
    }

    fun <Value> addDynamic(provider: StateItemBuilder<Owner>.(Owner) -> Unit) {
        val items = mutableListOf<PreservedStateItem<Owner, *, *>>()
        val nestedBuilder = StateItemBuilder { items += it }
        providers += PreservedStateItemProvider { owner ->
            nestedBuilder.provider(owner)
            return@PreservedStateItemProvider items
        }
    }

    fun build(): StateKeeper<Owner> {
        return StateKeeper(providers)
    }
}

internal fun <Delegate : Any, Owner : Delegate, Node : Any, Value> StateKeeperBuilder<Delegate, Owner>.addList(
    mapper: (Owner) -> List<Node>,
    provider: (Node) -> Value,
    mutator: (Node, Value) -> Unit,
    arranger: ((Node) -> Value)? = null
) {
    addDynamic<Value> { owner ->
        for (node in mapper(owner)) {
            if (provider(node) != null) {
                addNested({ node }, provider, mutator, arranger)
            }
        }
    }
}

internal fun interface PreservedStateItemProvider<in Owner : Any> {
    fun get(owner: Owner): List<PreservedStateItem<Owner, *, *>>
}

internal class StateKeeper<in Owner : Any>(val providers: List<PreservedStateItemProvider<Owner>>) {
    fun arrange(owner: Owner): PreservedState {
        val stamps = providers.flatMap { it.get(owner) }.map { it.prepare(owner) }

        return object : PreservedState {
            private val isRestored = AtomicBoolean(false)

            override fun restore() {
                if (!isRestored.compareAndSet(false, true)) return
                stamps.forEach { it.restore() }
            }
        }
    }
}

internal fun <Target : Any, Result> resolve(target: Target, keeper: StateKeeper<Target>, block: () -> Result): Result {
    var preservedState: PreservedState? = null

    var isSuccessful = false
    try {
        preservedState = keeper.arrange(target)
        val result = block()
        isSuccessful = true
        return result
    } finally {
        if (!isSuccessful) {
            preservedState?.restore()
        }
    }
}

internal interface PreservedState {
    companion object Empty : PreservedState {
        override fun restore() {}
    }

    fun restore()
}