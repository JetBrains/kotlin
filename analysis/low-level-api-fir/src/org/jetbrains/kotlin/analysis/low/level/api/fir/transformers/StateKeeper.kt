/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

@DslMarker
internal annotation class StateKeeperDsl

/**
 * Post-processors run after arrangers, but before the action block.
 * It is a work-around for cases when an arranger cannot properly tweak the state by itself
 * (for instance, when such tweak depends on a non-local mutation).
 */
internal typealias PostProcessor = () -> Unit

@StateKeeperDsl
internal interface StateKeeperBuilder {
    fun register(state: PreservedState)
}

@JvmInline
@StateKeeperDsl
internal value class StateKeeperScope<Owner : Any, Context : Any>(private val owner: Owner) {
    /**
     * Defines an entity state preservation rule.
     *
     * @param provider a function that returns the current entity state (a getter).
     * @param mutator a function that modifies the entity state (a setter).
     * @param arranger a function that provides a tweaked entity state. Such a state is then applied using a [mutator].
     */
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

    /**
     * Defines an entity state preservation rule.
     *
     * @param provider a function that returns the current entity state (a getter).
     * @param mutator a function that modifies the entity state (a setter).
     */
    context(StateKeeperBuilder)
    inline fun <Value> add(provider: (Owner) -> Value, crossinline mutator: (Owner, Value) -> Unit) {
        val owner = this@StateKeeperScope.owner
        val storedValue = provider(owner)

        register(object : PreservedState {
            override fun restore() = mutator(owner, storedValue)
            override fun postProcess() {}
        })
    }

    /**
     * Defines an entity state preservation rule by delegating to a given [keeper].
     * In other words, applies all rules defined in the [keeper] to the current entity.
     */
    context(StateKeeperBuilder)
    fun add(keeper: StateKeeper<Owner, Context>, context: Context) {
        val owner = this@StateKeeperScope.owner
        register(keeper.prepare(owner, context))
    }

    /**
     * Defines a post-processor.
     */
    context(StateKeeperBuilder)
    fun postProcess(block: PostProcessor) {
        register(object : PreservedState {
            override fun postProcess() = block()
            override fun restore() {}
        })
    }
}

/**
 * Registers a given [entity] using the delegate [keeper].
 * Does nothing if the [entity] is `null`.
 */
context(StateKeeperBuilder)
internal fun <Entity : Any, Context : Any> entity(entity: Entity?, keeper: StateKeeper<Entity, Context>, context: Context) {
    if (entity != null) {
        StateKeeperScope<Entity, Context>(entity).add(keeper, context)
    }
}

/**
 * Registers a given [entity] using the building [block].
 * Does nothing if the [entity] is `null`.
 */
context(StateKeeperBuilder)
internal inline fun <Entity : Any, Context : Any> entity(
    entity: Entity?,
    context: Context,
    block: StateKeeperScope<Entity, Context>.(Entity, Context) -> Unit,
) {
    if (entity != null) {
        StateKeeperScope<Entity, Context>(entity).block(entity, context)
    }
}

/**
 * Registers all entities in a given [list] sequentially using the delegate [keeper].
 * Does nothing if the [list] is `null`.
 * Skips `null` elements in the [list].
 */
context(StateKeeperBuilder)
internal fun <Entity : Any, Context : Any> entityList(list: List<Entity?>?, keeper: StateKeeper<Entity, Context>, context: Context) {
    if (list != null) {
        for (entity in list) {
            if (entity != null) {
                StateKeeperScope<Entity, Context>(entity).add(keeper, context)
            }
        }
    }
}

/**
 * Registers all entities in a given [list] sequentially using the building [block].
 * Does nothing if the [list] is `null`.
 * Skips `null` elements in the [list].
 */
context(StateKeeperBuilder)
internal inline fun <Entity : Any, Context : Any> entityList(
    list: List<Entity?>?,
    context: Context,
    block: StateKeeperScope<Entity, Context>.(Entity, Context) -> Unit,
) {
    if (list != null) {
        for (entity in list) {
            if (entity != null) {
                StateKeeperScope<Entity, Context>(entity).block(entity, context)
            }
        }
    }
}

/**
 * Defines a [StateKeeper] using a builder DSL.
 * This function is supposed to be the main entry point for [StateKeeper] creation.
 *
 * @param block a function that defines state preservation rules.
 *  The function collects rules for each individual owner separately.
 *  Nested owners can be handled inside [entity] or [entityList] blocks.
 */
internal fun <Owner : Any, Context : Any> stateKeeper(block: context(StateKeeperBuilder) StateKeeperScope<Owner, Context>.(Owner, Context) -> Unit): StateKeeper<Owner, Context> {
    return StateKeeper { owner, context ->
        val states = mutableListOf<PreservedState>()

        val builder = object : StateKeeperBuilder {
            override fun register(state: PreservedState) {
                states += state
            }
        }

        val scope = StateKeeperScope<Owner, Context>(owner)
        block(builder, scope, owner, context)

        object : PreservedState {
            private var isPostProcessed = false
            private var isRestored = false

            override fun postProcess() {
                if (isPostProcessed) {
                    return
                }

                isPostProcessed = true
                states.forEach { it.postProcess() }
            }

            override fun restore() {
                if (isRestored) {
                    return
                }

                isRestored = true
                states.forEach { it.restore() }
            }
        }
    }
}

/**
 * [StateKeeper] backs up parts of an object state, and allows to restore it if errors occur during that object mutation.
 * This is not a complete object dumper. All preservation rules are explicitly defined (usually in the [stateKeeper] DSL).
 *
 * State keeper provides the following life cycle:
 * 1. Preparation. Back up the state.
 * 2. Arrangement. For rules with arrangers, use the state provider by the arranger.
 * 3. Post-processing. Call supplied post-processors for additional state tweaks.
 * 4. Action. Perform the potentially failing action.
 * 5. Restoration (optional). Restore the state if the action failed.
 *
 * Preparation and arrangement steps are run by calling the [prepare] function.
 * Post-processing is run by calling [PreservedState.postProcess], potentially after some manual tweaks.
 * Restoration is run by calling [PreservedState.restore] if the action failed.
 *
 * @sample
 * ```
 * var state: PreservedState? = null
 *
 * try {
 *     state = keeper.prepare(owner)
 *     state.postProcess()
 *     action(owner)
 * } catch (e: Throwable) {
 *     state?.restore()
 * }
 * ```
 */
internal class StateKeeper<in Owner : Any, Context : Any>(val provider: (Owner, Context) -> PreservedState) {
    /**
     * Backs up the [owner] state by calling providers, and potentially tweaks the object state with arrangers.
     * Note that post-processors are not run during preparation.
     */
    fun prepare(owner: Owner, context: Context): PreservedState {
        return provider(owner, context)
    }
}

/**
 * State preserved by a [StateKeeper].
 */
internal interface PreservedState {
    /**
     * Performs post-processing of the state, according to supplied [PostProcessor]s.
     * This function must be called before the potentially failing action block.
     */
    fun postProcess()

    /**
     * Restores the state, according to rules defined by the [StateKeeper].
     * This function must be called when the action block fails.
     */
    fun restore()
}

internal inline fun <Target : FirElementWithResolveState, Context : Any, Result> resolveWithKeeper(
    target: Target,
    context: Context,
    keeper: StateKeeper<Target, Context>,
    prepareTarget: (Target) -> Unit = {},
    action: () -> Result,
): Result {
    val onAirAnalysisTarget = target.moduleData.session.onAirAnalysisTarget
    if (onAirAnalysisTarget != null && target in onAirAnalysisTarget) {
        // Several arrangers reset declaration bodies to lazy ones, and then re-construct the FIR tree from the backing PSI.
        // This won't work for on-air analysis, as the tree is manually modified. However, it doesn't seem that tree guards are needed
        // in the first place, as results of the on-air analysis aren't going to be shared.
        prepareTarget(target)
        return action()
    }

    var preservedState: PreservedState? = null

    try {
        preservedState = keeper.prepare(target, context)
        prepareTarget(target)
        preservedState.postProcess()
        return action()
    } catch (e: Throwable) {
        preservedState?.restore()
        throw e
    }
}

private operator fun FirElementWithResolveState.contains(child: FirElementWithResolveState): Boolean {
    if (this == child) {
        return true
    }

    var isFound = false

    accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (!isFound) {
                if (element == child) {
                    isFound = true
                } else if (element is FirDeclaration || element !is FirStatement) {
                    element.acceptChildren(this)
                }
            }
        }
    })

    return isFound
}