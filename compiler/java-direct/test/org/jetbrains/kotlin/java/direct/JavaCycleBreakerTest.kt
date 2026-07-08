/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.resolve.providers.FirNullSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.java.direct.resolution.cycleGuardedSupertypeWalk
import org.jetbrains.kotlin.java.direct.resolution.cycleSafeClassLikeSymbol
import org.jetbrains.kotlin.java.direct.resolution.registerJavaModelInFlightResolutionsIfAbsent
import org.jetbrains.kotlin.java.direct.resolution.registerJavaModelSupertypeWalkGuardIfAbsent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Targeted regression tests for the module's two independent cycle breakers:
 *
 *  - [org.jetbrains.kotlin.java.direct.resolution.cycleGuardedSupertypeWalk] backed by
 *    [org.jetbrains.kotlin.java.direct.resolution.JavaModelSupertypeWalkGuard] — bounds Java
 *    inheritance-graph cycles (`A extends B`, `B extends A`) when
 *    [org.jetbrains.kotlin.java.direct.resolution.directSupertypeClassIds] re-enters itself for
 *    the same `ClassId` (e.g. via a supertype's own `.classifier` resolution looping back).
 *  - [FirSession.cycleSafeClassLikeSymbol] backed by
 *    [org.jetbrains.kotlin.java.direct.resolution.JavaModelInFlightResolutions] — breaks the
 *    `FirJavaClass.declarations` PUBLICATION-lazy re-entrance cycle (KT-74097), where a
 *    symbol-provider lookup materialises declarations that probe the same `ClassId` again.
 *
 * Each breaker has a pair of tests: one showing the breaker terminates the cycle, and one
 * showing the same path without the breaker exhausts the stack — proving the guard is
 * load-bearing rather than dead code.
 */
class JavaCycleBreakerTest {
    private val a = ClassId(FqName("test"), Name.identifier("A"))
    private val b = ClassId(FqName("test"), Name.identifier("B"))

    // --- cycleGuardedSupertypeWalk / JavaModelSupertypeWalkGuard --------------------------------
    //
    // Hypothetical code / usage pattern that invokes this breaker:
    //
    //   // A.java
    //   public class A extends B {
    //       public Nested f() { return null; }   // `Nested` is inherited from B
    //   }
    //   // B.java
    //   public class B extends A {                // <-- malformed: A and B extend each other
    //       public static class Nested {}
    //   }
    //
    // This is *invalid* Java (a `cyclic inheritance` error), but the model layer still has to walk
    // the supertype graph during error recovery — e.g. `directSupertypeClassIds(A)`'s source arm
    // reads A's supertype `B` via its `.classifier`, whose own resolution can loop back into
    // `directSupertypeClassIds(A)` again before the first call returns. Without a bound the walk
    // recurses A -> B -> A -> ... until a StackOverflowError. `cycleGuardedSupertypeWalk` marks each
    // ClassId in-flight for the duration of its walk, so re-entering an already-active ClassId
    // returns the caller's default and the walk terminates. (In practice such Java cycles are
    // usually pre-bounded by FIR's `SupertypeComputationStatus.Computing` sentinel before the
    // model-side walk re-enters, so this guard is a defense-in-depth net for degenerate
    // error-recovery paths.)

    @OptIn(SessionConfiguration::class)
    @Test
    fun testSupertypeWalkGuardBreaksReentrantWalk() {
        val session = createDummyFirSessionForTests()
        // Register the per-session supertype-walk guard — this is what bounds the cyclic walk.
        session.registerJavaModelSupertypeWalkGuardIfAbsent()

        // Cyclic Java inheritance: A extends B, B extends A.
        val directSupertypes = mapOf(a to listOf(b), b to listOf(a))

        var visits = 0

        // Mirrors how directSupertypeClassIds can recurse into itself for the same ClassId
        // under session.cycleGuardedSupertypeWalk(...).
        fun walk(classId: ClassId): Unit = session.cycleGuardedSupertypeWalk(classId, default = Unit) {
            visits++
            for (supertype in directSupertypes.getValue(classId)) {
                walk(supertype)
            }
        }

        walk(a)

        assert(visits == 2) {
            "Each class in the cycle must be entered exactly once; re-entry is broken by the guard, got $visits"
        }
    }

    @OptIn(SessionConfiguration::class)
    @Test
    fun testSupertypeWalkWithoutGuardStackOverflows() {
        val session = createDummyFirSessionForTests()
        // Intentionally do NOT register JavaModelSupertypeWalkGuard, so cycleGuardedSupertypeWalk
        // cannot mark a ClassId in-flight and the cyclic walk is unbounded.
        val directSupertypes = mapOf(a to listOf(b), b to listOf(a))

        fun walk(classId: ClassId): Unit = session.cycleGuardedSupertypeWalk(classId, default = Unit) {
            for (supertype in directSupertypes.getValue(classId)) {
                walk(supertype)
            }
        }

        // Without the guard the walk recurses A -> B -> A -> ... until the stack is exhausted,
        // demonstrating the session guard is what makes the cyclic walk terminate.
        assertThrows<StackOverflowError> { walk(a) }
    }

    // --- cycleSafeClassLikeSymbol / JavaModelInFlightResolutions -------------------------------

    // What this breaker protects in the real IntelliJ full-pipeline scenario
    // (`IntelliJFullPipelineTestsGenerated.testIntellij_vcs_git`:
    //
    // `community/plugins/git4idea/src/git4idea/commands/GitSimpleEventDetector.java` has a nested
    // enum `Event` with a constant annotated `@Deprecated`. Resolving the simple name `Deprecated`
    // produces the enclosing-qualified candidate `ClassId` GitSimpleEventDetector.Event.Deprecated.
    // Because that module has compiler plugins enabled, the candidate is probed through
    // `FirExtensionDeclarationsSymbolProvider` -> `FirNestedClassifierScopeImpl`, which forces
    // `FirJavaClass.declarations` — a PUBLICATION lazy that re-runs on same-thread re-entrance
    // (KT-74097). Materialising those declarations re-converts the same `@Deprecated` field
    // (`convertJavaFieldToFir` / `setAnnotationsFromJava`), which re-resolves the very same
    // candidate `ClassId` -> a self-cycle. `cycleSafeClassLikeSymbol` marks the ClassId in-flight
    // on the first probe, so the second (re-entrant) probe short-circuits to `null` and resolution
    // falls back to `java.lang.Deprecated` instead of crashing with a StackOverflowError.
    //
    // The test below reproduces that re-entrance with a minimal stub provider rather than the heavy
    // full-pipeline module: the stub's lookup calls back into `cycleSafeClassLikeSymbol` for the
    // same ClassId, standing in for the declarations-materialisation re-entrance above.

    @OptIn(SessionConfiguration::class)
    @Test
    fun testInFlightGuardBreaksReentrantSymbolLookup() {
        val session = createDummyFirSessionForTests()
        // Register the in-flight guard component — this is what enables the re-entrance break.
        session.registerJavaModelInFlightResolutionsIfAbsent()

        var providerInvocations = 0
        val provider = ReentrantStubSymbolProvider(session) { classId ->
            providerInvocations++
            // Simulate the KT-74097 PUBLICATION-lazy re-entrance: materialising this class's
            // declarations probes the very same ClassId again through the model chokepoint.
            session.cycleSafeClassLikeSymbol(classId)
        }
        session.register(FirSymbolProvider::class, provider)

        val result = session.cycleSafeClassLikeSymbol(a)

        assert(result == null) {
            "The re-entrant probe for an in-flight ClassId must be short-circuited to null, got $result"
        }
        assert(providerInvocations == 1) {
            "The provider must be entered exactly once; the re-entrant probe for the same in-flight " +
                    "ClassId short-circuits before reaching the provider again, got $providerInvocations"
        }
    }

    @OptIn(SessionConfiguration::class)
    @Test
    fun testReentrantSymbolLookupWithoutInFlightGuardStackOverflows() {
        val session = createDummyFirSessionForTests()
        // Intentionally do NOT register JavaModelInFlightResolutions, so the guard is disabled and
        // cycleSafeClassLikeSymbol cannot mark the ClassId as in-flight.
        val provider = ReentrantStubSymbolProvider(session) { classId ->
            session.cycleSafeClassLikeSymbol(classId)
        }
        session.register(FirSymbolProvider::class, provider)

        assertThrows<StackOverflowError> { session.cycleSafeClassLikeSymbol(a) }
    }
}

/**
 * Minimal [FirSymbolProvider] whose [getClassLikeSymbolByClassId] delegates to [onLookup], used to
 * reproduce the re-entrant symbol-provider lookup that [FirSession.cycleSafeClassLikeSymbol] guards
 * against. All other provider responsibilities are stubbed empty.
 */
@OptIn(FirSymbolProviderInternals::class)
private class ReentrantStubSymbolProvider(
    session: FirSession,
    private val onLookup: (ClassId) -> FirClassLikeSymbol<*>?,
) : FirSymbolProvider(session) {
    override val symbolNamesProvider: FirSymbolNamesProvider get() = FirNullSymbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? = onLookup(classId)

    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    override fun hasPackage(fqName: FqName): Boolean = false
}
