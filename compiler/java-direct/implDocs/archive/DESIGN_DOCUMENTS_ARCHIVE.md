# Java-Direct: Archived Design Documents

## Document Purpose

This archive contains the design analysis documents that guided iterations 1-6 of the java-direct module implementation. These documents are preserved for historical reference and deep context recovery if needed.

**Status**: Archived  
**Date Archived**: 2026-03-03

---

## Archived Documents

The following design documents were used during iterations 1-6 and are now archived:

1. **FIRSESSION_RESOLUTION_ANALYSIS.md** - Analysis of how type resolution should access FirSession
2. **TYPE_RESOLUTION_DESIGN.md** - Design for star imports and java.lang resolution via callback
3. **EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md** - Analysis of binary dependency resolution gaps

---

## Document Summaries

### FIRSESSION_RESOLUTION_ANALYSIS.md

**Key Decision**: Java Model provides names, FIR provides resolution. No FirSession access in Java Model.

**Core Insight**: FIR's `JavaTypeConversion.kt` explicitly handles `classifier == null` case by using `classifierQualifiedName`.

**Solution Chosen**: Solution 1 - No Resolution in Java Model
- `JavaClassifierType.classifier` returns `null` for external types
- `JavaClassifierType.classifierQualifiedName` returns the type name as string
- FIR layer handles all external resolution

### TYPE_RESOLUTION_DESIGN.md

**Problem Solved**: Star imports (`import java.util.*;`) and automatic java.lang import weren't being resolved.

**Solution**: Callback approach added to `JavaClassifierType`:
```kotlin
val isResolved: Boolean get() = true
fun resolve(tryResolve: (String) -> Boolean): String? = null
```

**Resolution Order** (per JLS):
1. java.lang.* first (implicit import)
2. Explicit star imports in order
3. Ambiguity detection (return null if found in multiple)

### EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md

**Problem Identified**: java-direct only replaced `JavaClassFinder` for sources, but binary class resolution (JDK, libraries) still needed platform infrastructure.

**Architecture Gap**:
- java-direct handles: Java sources (.java files)
- java-direct didn't handle: Binary classes (.class in JARs), JDK, libraries

**Solution**: Hybrid `CombinedJavaClassFinder`:
1. Try source finder (java-direct) first
2. Fall back to platform binary finder

### FOCUS_STRATEGY.md

**Methodology**: Single-test-driven development approach to avoid analysis paralysis.

**The One Test Rule**: Pick ONE concrete failing test, make it pass, measure impact.

**Workflow**:
1. Get error statistics (5 min)
2. Find simplest test with most common error
3. Deep dive on that ONE test
4. Create unit test reproducing issue
5. Fix and verify
6. Measure impact on similar tests

---

## When to Restore Full Context

These archived documents should only be consulted if:

1. **Debugging regression** - Need to understand original design intent
2. **Major refactoring** - Revisiting fundamental architecture decisions
3. **New similar feature** - Implementing something that follows same patterns
4. **Onboarding** - New developer needs deep historical context

For ongoing iteration 7+ work, the summaries above should suffice.

---

## Original Document Locations (Before Archive)

- `compiler/java-direct/FIRSESSION_RESOLUTION_ANALYSIS.md`
- `compiler/java-direct/TYPE_RESOLUTION_DESIGN.md`
- `compiler/java-direct/EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md`
- `compiler/java-direct/FOCUS_STRATEGY.md`

These files have been deleted after archiving to reduce context overhead.
