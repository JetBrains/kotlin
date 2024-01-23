/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirLazyResolveContractChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.lockWithPCECheck
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.locks.ReentrantLock

/**
 * Keyed locks provider.
 */
internal class LLFirLockProvider(private val checker: LLFirLazyResolveContractChecker) {
    private val globalLock = ReentrantLock()

    private val implicitTypesLock = ReentrantLock()

    inline fun <R> withGlobalLock(
        lockingIntervalMs: Long = DEFAULT_LOCKING_INTERVAL,
        action: () -> R,
    ): R {
        if (!globalLockEnabled) return action()

        return globalLock.lockWithPCECheck(lockingIntervalMs, action)
    }

    fun withGlobalPhaseLock(
        phase: FirResolvePhase,
        action: () -> Unit,
    ) {
        if (!implicitPhaseLockEnabled) return action()

        val lock = when (phase) {
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> implicitTypesLock
            else -> null
        }

        if (lock == null) {
            action()
        } else {
            lock.lockWithPCECheck(DEFAULT_LOCKING_INTERVAL, action)
        }
    }

    /**
     * A contract violation check to be sure that we won't request a violated phase later.
     * This is useful to catch a contract violation for jumping phases because they may encounter infinite recursion.
     *
     * Example: we have cycle between phases 'implicit type (1) -> body (2) -> implicit type (3)` and
     * we can get [StackOverflowError] because regular phases checks can't catch such case
     * because will check only implicit type -> implicit type resolution due to
     * sequent resolution requests
     *
     * @see org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirModuleLazyDeclarationResolver
     */
    fun checkContractViolations(toPhase: FirResolvePhase) {
        checker.checkIfCanLazyResolveToPhase(toPhase, isJumpingPhase = true)
    }

    /**
     * Locks an a [FirElementWithResolveState] to resolve from `phase - 1` to [phase] and
     * then updates the [FirElementWithResolveState.resolveState] to a [phase].
     * Does nothing if [target] already has at least [phase] phase.
     *
     * [action] will be executed once if [target] is not yet resolved to [phase] phase.
     *
     * @see withReadLock
     * @see withJumpingLock
     */
    inline fun withWriteLock(
        target: FirElementWithResolveState,
        phase: FirResolvePhase,
        action: () -> Unit,
    ) {
        checker.lazyResolveToPhaseInside(phase) {
            target.withLock(toPhase = phase, updatePhase = true, action = action)
        }
    }

    /**
     * Locks an a [FirElementWithResolveState] to read something required for [phase].
     * Does nothing if [target] already has at least [phase] phase.
     *
     * [action] will be executed once if [target] is not yet resolved to [phase] phase.
     *
     * @see withWriteLock
     */
    inline fun withReadLock(
        target: FirElementWithResolveState,
        phase: FirResolvePhase,
        action: () -> Unit,
    ) {
        checker.lazyResolveToPhaseInside(phase) {
            target.withLock(toPhase = phase, updatePhase = false, action = action)
        }
    }

    /**
     * Locks an a [FirElementWithResolveState] to resolve from `toPhase - 1` to [toPhase] and
     * then updates the [FirElementWithResolveState.resolveState] to a [toPhase] if [updatePhase] is **true**.
     *
     * [updatePhase] == false means that we want to read some data under a lock.
     *
     * If [FirElementWithResolveState] is already at least at [toPhase], does nothing.
     *
     * Otherwise:
     *  - Marks [FirElementWithResolveState] as in a process of resovle
     *  - performs the resolve by calling [action]
     *  - updates the resolve phase to [toPhase] if [updatePhase] is **true**.
     *  - notifies other threads waiting on the same lock that the declaration is already resolved by this thread, so other threads can continue its execution.
     *
     *
     *  Contention handling:
     *  - on lock acquisition, no real lock or barrier is created. Instead, the [FirElementWithResolveState.resolveState] is updated to indicate that the declaration is being resolved now.
     *  - If some other thread tries to resolve current [FirElementWithResolveState], it changes `resolveState` and puts the barrier there. Then it awaits on it until the initial thread which hold the lock finishes its job.
     *  - This way, no barrier is used in a case when no contention arise.
     */
    private inline fun FirElementWithResolveState.withLock(
        toPhase: FirResolvePhase,
        updatePhase: Boolean,
        action: () -> Unit,
    ) {
        while (true) {
            checkCanceled()

            @OptIn(ResolveStateAccess::class)
            val stateSnapshot = resolveState
            if (stateSnapshot.resolvePhase >= toPhase) {
                // already resolved by some other thread
                return
            }

            when (stateSnapshot) {
                is FirInProcessOfResolvingToPhaseStateWithoutBarrier -> {
                    // some thread is resolving the phase, so we wait until it finishes
                    trySettingBarrier(toPhase, stateSnapshot)
                    continue
                }

                is FirInProcessOfResolvingToPhaseStateWithBarrier -> {
                    // some thread is waiting on a barrier as the declaration is being resolved, so we try too
                    waitOnBarrier(stateSnapshot)
                    continue
                }

                is FirResolvedToPhaseState -> {
                    if (!tryLock(toPhase, stateSnapshot)) continue

                    var exceptionOccurred = false
                    try {
                        action()
                    } catch (e: Throwable) {
                        exceptionOccurred = true
                        throw e
                    } finally {
                        val newPhase = if (updatePhase && !exceptionOccurred) toPhase else stateSnapshot.resolvePhase
                        unlock(toPhase = newPhase)
                    }

                    return
                }

                is FirInProcessOfResolvingToJumpingPhaseState -> {
                    errorWithFirSpecificEntries("$stateSnapshot state are not allowed to be inside non-jumping lock", fir = this)
                }
            }
        }
    }

    private fun waitOnBarrier(
        stateSnapshot: FirInProcessOfResolvingToPhaseStateWithBarrier,
    ): Boolean {
        return stateSnapshot.barrier.await(DEFAULT_LOCKING_INTERVAL, TimeUnit.MILLISECONDS)
    }

    private fun FirElementWithResolveState.trySettingBarrier(
        toPhase: FirResolvePhase,
        stateSnapshot: FirResolveState,
    ) {
        val newState = FirInProcessOfResolvingToPhaseStateWithBarrier(toPhase)
        resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
    }

    private fun FirElementWithResolveState.tryLock(
        toPhase: FirResolvePhase,
        stateSnapshot: FirResolveState,
    ): Boolean {
        val newState = FirInProcessOfResolvingToPhaseStateWithoutBarrier(toPhase)
        return resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
    }

    private fun FirElementWithResolveState.unlock(toPhase: FirResolvePhase) {
        when (val stateSnapshotAfter = resolveStateFieldUpdater.getAndSet(this, FirResolvedToPhaseState(toPhase))) {
            is FirInProcessOfResolvingToPhaseStateWithoutBarrier -> {}
            is FirInProcessOfResolvingToPhaseStateWithBarrier -> {
                stateSnapshotAfter.barrier.countDown()
            }
            is FirResolvedToPhaseState, is FirInProcessOfResolvingToJumpingPhaseState -> {
                errorWithFirSpecificEntries("phase is unexpectedly unlocked $stateSnapshotAfter", fir = this)
            }
        }
    }

    /**
     * Locks on an a [FirElementWithResolveState] to resolve from `phase - 1` to [phase] and
     * then updates the [resolve state][FirElementWithResolveState.resolveState] to a [phase].
     * Does nothing if [target] already has at least [phase] phase.
     *
     * @param actionUnderLock will be executed once under the lock if [target] is not yet resolved to [phase] phase and there are no cycles
     * @param actionOnCycle will be executed once without the lock if [target] is not yet resolved to [phase] phase and a resolution cycle is found
     *
     * @see withWriteLock
     * @see withJumpingLockImpl
     */
    fun withJumpingLock(
        target: FirElementWithResolveState,
        phase: FirResolvePhase,
        actionUnderLock: () -> Unit,
        actionOnCycle: () -> Unit,
    ) {
        checker.lazyResolveToPhaseInside(phase, isJumpingPhase = true) {
            target.withJumpingLockImpl(phase, actionUnderLock, actionOnCycle)
        }
    }

    /**
     * Holds resolution states of the current thread.
     * This information is required to properly process possible cycles
     * during resolution.
     *
     * @see withJumpingLockImpl
     * @see tryJumpingLock
     * @see jumpingUnlock
     */
    private val jumpingResolutionStatesStack = JumpingResolutionStatesStack()

    /**
     * Locks an a [FirElementWithResolveState] to resolve from `toPhase - 1` to [toPhase] and
     * then updates the [FirElementWithResolveState.resolveState] to a
     * [toPhase] if no exceptions were found during [actionUnderLock].
     *
     * If [FirElementWithResolveState] is already at least at [toPhase], does nothing.
     *
     * ### Happy path:
     *  1. Marks [FirElementWithResolveState] as in a process of resolve
     *  2. Performs the resolve by calling [actionUnderLock]
     *  3. Updates the resolve phase to [toPhase] if there is no exceptions
     *  4. Notifies other threads waiting on the same lock that this thread already resolved the declaration,
     *  so other threads can continue its execution
     *
     *  ### Cycle handling
     *  During step 1 we can realize someone already set [FirInProcessOfResolvingToJumpingPhaseState]
     *  for the current [FirElementWithResolveState], so there is a room for a possible deadlock.
     *
     *  The requirement for the deadlock is not empty [jumpingResolutionStatesStack] as we should already hold another lock.
     *  Otherwise, we can just wait on the [latch][FirInProcessOfResolvingToJumpingPhaseState.latch].
     *
     *  In the case of not empty [jumpingResolutionStatesStack], we have the following algorithm:
     *  1. Set [waitingFor][FirInProcessOfResolvingToJumpingPhaseState.waitingFor] for the previous state
     *  as we have an intention to take the next lock
     *  2. Iterate over all [waitingFor][FirInProcessOfResolvingToJumpingPhaseState.waitingFor] recursively
     *  to detect the possible cycle
     *  3. Execute [actionOnCycle] without the lock in the case of cycle or waining on
     *  the [latch][FirInProcessOfResolvingToJumpingPhaseState.latch] to try to take the lock again later
     *
     * @param actionUnderLock will be executed once under the lock if [this] is not yet resolved to [toPhase] phase and there are no cycles
     * @param actionOnCycle will be executed once without the lock if [this] is not yet resolved to [toPhase] phase and a resolution cycle is found
     *
     *  @see withJumpingLock
     */
    private fun FirElementWithResolveState.withJumpingLockImpl(
        toPhase: FirResolvePhase,
        actionUnderLock: () -> Unit,
        actionOnCycle: () -> Unit,
    ) {
        while (true) {
            checkCanceled()

            @OptIn(ResolveStateAccess::class)
            val currentState = resolveState
            if (currentState.resolvePhase >= toPhase) {
                // already resolved by some other thread
                return
            }

            when (currentState) {
                is FirResolvedToPhaseState -> {
                    if (!tryJumpingLock(toPhase, currentState)) continue

                    var exceptionOccurred = false
                    try {
                        actionUnderLock()
                    } catch (e: Throwable) {
                        exceptionOccurred = true
                        throw e
                    } finally {
                        val newPhase = if (!exceptionOccurred) toPhase else currentState.resolvePhase
                        jumpingUnlock(toPhase = newPhase)
                    }

                    return
                }

                is FirInProcessOfResolvingToJumpingPhaseState -> {
                    val previousState = jumpingResolutionStatesStack.peek()

                    // Not null value means we already hold a lock for another declaration in the current thread,
                    // so we have to check the possible cycle
                    if (previousState != null) {
                        // All writing to waitingFor will be consistent, as it is the last writing if we have cycle
                        previousState.waitingFor = currentState

                        // Cycle check
                        var nextState: FirInProcessOfResolvingToJumpingPhaseState? = currentState
                        while (nextState != null) {
                            if (nextState === previousState) {
                                previousState.waitingFor = null
                                return actionOnCycle()
                            }

                            nextState = nextState.waitingFor
                        }
                    }

                    try {
                        // Waiting until another thread released the lock
                        currentState.latch.await(DEFAULT_LOCKING_INTERVAL, TimeUnit.MILLISECONDS)
                    } finally {
                        previousState?.waitingFor = null
                    }
                }

                is FirInProcessOfResolvingToPhaseStateWithoutBarrier, is FirInProcessOfResolvingToPhaseStateWithBarrier -> {
                    errorWithFirSpecificEntries("$currentState state are not allowed to be inside jumping lock", fir = this)
                }
            }
        }
    }

    /**
     * Trying to set [FirInProcessOfResolvingToJumpingPhaseState] to [this].
     *
     * @return **true** if the state is published successfully
     *
     * @see withJumpingLockImpl
     * @see FirInProcessOfResolvingToJumpingPhaseState
     */
    private fun FirElementWithResolveState.tryJumpingLock(
        toPhase: FirResolvePhase,
        stateSnapshot: FirResolveState,
    ): Boolean {
        val newState = FirInProcessOfResolvingToJumpingPhaseState(toPhase)
        val isSucceed = resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
        if (!isSucceed) return false

        jumpingResolutionStatesStack.push(newState)

        return true
    }

    /**
     * Publish [FirResolvedToPhaseState] with [toPhase] phase and unlocks current [FirInProcessOfResolvingToJumpingPhaseState].
     *
     * @see withJumpingLockImpl
     * @see FirInProcessOfResolvingToJumpingPhaseState
     * @see FirResolvedToPhaseState
     */
    private fun FirElementWithResolveState.jumpingUnlock(toPhase: FirResolvePhase) {
        val currentState = jumpingResolutionStatesStack.pop()

        resolveStateFieldUpdater.set(this, FirResolvedToPhaseState(toPhase))
        currentState.latch.countDown()
    }
}

private val resolveStateFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
    FirElementWithResolveState::class.java,
    FirResolveState::class.java,
    "resolveState"
)

private val globalLockEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Registry.`is`("kotlin.parallel.resolve.under.global.lock", false)
}

private val implicitPhaseLockEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Registry.`is`("kotlin.implicit.resolve.phase.under.global.lock", false)
}

private const val DEFAULT_LOCKING_INTERVAL = 50L

/**
 * @see FirInProcessOfResolvingToJumpingPhaseState
 */
private class JumpingResolutionStatesStack {
    private val stateStackHolder = ThreadLocal.withInitial<MutableList<FirInProcessOfResolvingToJumpingPhaseState>> {
        mutableListOf()
    }

    /**
     * Adds [newState] to the stack and set [waitingFor][FirInProcessOfResolvingToJumpingPhaseState.waitingFor]
     * for the previous state if needed
     */
    fun push(newState: FirInProcessOfResolvingToJumpingPhaseState) {
        val states = stateStackHolder.get()

        val currentState = states.lastOrNull()
        currentState?.waitingFor = newState
        states += newState
    }

    /**
     * Pops from the top of the stack the last state and return it.
     * Updates [waitingFor][FirInProcessOfResolvingToJumpingPhaseState.waitingFor] for
     * the previous state if needed
     *
     * Note: it doesn't release the [lock][FirInProcessOfResolvingToJumpingPhaseState.latch]
     */
    fun pop(): FirInProcessOfResolvingToJumpingPhaseState {
        val states = stateStackHolder.get()

        val currentState = states.removeLast()
        val prevState = states.lastOrNull()
        requireWithAttachment(
            condition = prevState == null || prevState.waitingFor === currentState,
            message = { "The lock contact is violated" },
        )

        prevState?.waitingFor = null

        // Drop the empty stack to avoid memory leak
        // as the updated capacity of the stack can be high
        if (states.isEmpty()) {
            stateStackHolder.remove()
        }

        return currentState
    }

    /**
     * Current state on the top if exists
     */
    fun peek(): FirInProcessOfResolvingToJumpingPhaseState? = stateStackHolder.get().lastOrNull()
}