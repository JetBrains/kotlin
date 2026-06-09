# Java-Direct: Iterations 1-6 Archived Details

## Document Purpose

This archive contains the detailed iteration prompts and results from Iterations 1-6 of the java-direct module implementation. These iterations have been completed and this document serves as a reference for understanding the historical context and decisions made.

**Status**: Archived (Completed)  
**Iterations Covered**: 1-6  
**Final Test Result**: 90/138 (65.2%) box tests passing

---

## Summary of Completed Iterations

| Iteration | Focus | Result | Tests |
|-----------|-------|--------|-------|
| 1 | Initial Root Cause Analysis & Constructor Fix | Fixed `hasDefaultConstructor()` | 0→1/138 |
| 2 | Type Resolution Architecture | Verified classifierQualifiedName approach | 1/138 |
| 3 | Import Handling | Implemented JavaImports, simple imports | 1→11/138 |
| 4 | Star Import Resolution + Parameters | Callback approach + parameter parsing | 11→30/138 |
| 5 | Type Arguments Parsing | Implemented generic type arguments | 30→31/138 |
| 6 | Hybrid JavaClassFinder | Combined source+binary class finding | 31→90/138 |

---

## Iteration 1: Initial Root Cause Analysis

### Original Prompt

```
TASK: Find and deeply analyze ONE representative failing test

CONTEXT:
You are working on the Kotlin compiler's java-direct module. Many tests fail.
Your job: Pick ONE test, understand it completely, fix it.

CRITICAL MINDSET SHIFT:
- ❌ Don't analyze "patterns across all failures"
- ❌ Don't categorize 100+ test failures
- ✅ Pick ONE concrete failing test
- ✅ Make that ONE test pass
- ✅ Measure how many similar tests also pass
```

### Key Implementation

**Problem**: `JavaClassOverAst.hasDefaultConstructor()` was hardcoded to return `false`

**Fix**: Changed to `!isInterface && constructors.isEmpty()` matching PSI reference implementation

**Result**: Eliminated ALL 128 `UNRESOLVED_REFERENCE: '<init>'` errors

### Files Changed
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt:113`

---

## Iteration 2: Type Resolution - classifierQualifiedName

### Original Prompt

```
TASK: Fix JavaClassifierType to provide correct classifierQualifiedName for FIR resolution

CONTEXT:
Type resolution happens in the FIR layer, NOT in Java Model. Java Model's job is to:
1. Resolve LOCAL classes (same file) via LocalJavaScope → return in `classifier`
2. Provide correct type names via `classifierQualifiedName` → FIR resolves external types
```

### Key Implementation

**Architecture Validated**: Current implementation already followed correct approach from FIRSESSION_RESOLUTION_ANALYSIS.md

**Key Principle**: Java Model provides names, FIR provides resolution

**Result**: No test improvement expected - confirmed architecture is correct

### Files Changed
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` (minor fix for simple name extraction)

---

## Iteration 3: Import Handling and Name Qualification

### Original Prompt

```
TASK: Implement import statement tracking to improve classifierQualifiedName accuracy

CONTEXT:
After Iteration 2, we can extract type names but they're not qualified. When source says:
import java.util.ArrayList;
class MyClass extends ArrayList {}
We need `classifierQualifiedName` to return "java.util.ArrayList", not "ArrayList".
```

### Key Implementation

**Created**: `JavaImports` data class with `simpleImports: Map<String, FqName>` and `starImports: List<FqName>`

**Key Finding**: FqName must NOT contain asterisk (e.g., `java.util`, not `java.util.*`)

**Result**: 1/138 → 11/138 (10x improvement)

### Files Changed
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaImports.kt` (NEW)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt`

---

## Iteration 4: Star Import Resolution via Callback

### Original Prompt

```
TASK: Implement star import and java.lang resolution using the callback approach

CONTEXT:
After Iterations 2-3, we handle:
- ✅ Local classes (via `classifier`)
- ✅ Fully qualified names (`java.util.ArrayList`)
- ✅ Single-type imports (`import java.util.ArrayList;`)
- ❌ Star imports (`import java.util.*;` then `List`)
- ❌ java.lang automatic import (`Object` should resolve to `java.lang.Object`)
```

### Key Implementation

**Interface Changes** (in `javaTypes.kt`):
- Added `val isResolved: Boolean get() = true`
- Added `fun resolve(tryResolve: (String) -> Boolean): String? = null`

**Resolution Order** (per JLS):
1. java.lang.* first (implicit import)
2. Explicit star imports in order
3. Ambiguity detection (return null if found in multiple packages)

**Also Implemented**: Method/constructor parameter parsing (`JavaValueParameterOverAst`)

**Result**: 11/138 → 30/138

### Files Changed
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt`
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`

---

## Iteration 5: Basic Type Arguments Parsing

### Original Prompt

```
TASK: Find ONE test failing on type arguments, fix it, measure impact

CONTEXT:
**Current Status: 30/138 (21.7%) tests passing**

**Hypothesis**: Many remaining failures likely involve generics (`List<String>`, `Map<K,V>`), since `typeArguments` returns empty list.
```

### Key Implementation

**AST Discovery**: Type arguments are under `REFERENCE_PARAMETER_LIST` (not `TYPE_ARGUMENT_LIST`)

**Also Fixed**: Visibility ClassCastException - `JavaDescriptorVisibilities.PACKAGE_VISIBILITY` needed `.delegate`

**Critical Finding**: Type arguments were NOT the primary blocker - other issues (package discovery, JDK resolution) masked benefits

**Result**: 30/138 → 31/138 (minimal - type arguments correct but not the blocker)

### Files Changed
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` (visibility fix)

---

## Iteration 6: Hybrid JavaClassFinder for Binary Dependencies

### Original Prompt

```
TASK: Implement a hybrid JavaClassFinder that combines java-direct for sources with platform-based lookup for binaries

CONTEXT:
**Current Status: 31/138 (22.5%) tests passing**

Iteration 5 revealed a **fundamental architectural gap**:
1. **MISSING_DEPENDENCY_CLASS** (36 errors): Classes like `example.Hello` from test Java files fail to resolve
2. **UNRESOLVED_REFERENCE** for JDK classes (39 errors): `NullPointerException`, `RuntimeException`, `String`, `Object` etc.

**Root Cause**: java-direct only replaces JavaClassFinder for sources, but JDK/library binaries still need platform infrastructure.
```

### Key Implementation

**Solution**: `CombinedJavaClassFinder` that:
1. First tries `JavaClassFinderOverAstImpl` for Java sources
2. Falls back to platform-based `JavaClassFinder` for binary classes (JDK, libraries)

**Interface Change**: Added `defaultFinderProvider: (() -> JavaClassFinder)?` parameter to `JavaClassFinderFactory.createJavaClassFinder()`

**Bug Fix**: `ConcurrentHashMap` doesn't accept null values - only cache non-null results

**Result**: 31/138 → 90/138 (191% improvement!)

### Files Changed
- `compiler/cli/src/org/jetbrains/kotlin/cli/jvm/compiler/extensions/JavaClassFinderFactory.kt`
- `compiler/cli/src/org/jetbrains/kotlin/cli/jvm/compiler/VfsBasedProjectEnvironment.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaDirectComponentRegistrar.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt`

---

## Key Architectural Decisions Made

### 1. Type Resolution in FIR Layer (Iteration 2)
Java Model provides names (`classifierQualifiedName`), FIR provides resolution. No `FirSession` access in Java Model.

### 2. Callback Pattern for Star Imports (Iteration 4)
`resolve(tryResolve: (String) -> Boolean)` allows Java Model to implement Java resolution rules while FIR validates existence.

### 3. Hybrid Finder Architecture (Iteration 6)
Source-first, binary-fallback pattern allows java-direct for sources while maintaining platform infrastructure for binaries.

---

## Design Documents Referenced

These documents provided architectural guidance for iterations 1-6:

- `FIRSESSION_RESOLUTION_ANALYSIS.md` - Why Java Model shouldn't access FirSession
- `TYPE_RESOLUTION_DESIGN.md` - Callback approach for star imports
- `EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md` - Why hybrid finder was needed
- `FOCUS_STRATEGY.md` - Single-test-driven development approach

---

## Lessons Learned

1. **Focus on ONE test** - Analysis paralysis from trying to categorize all failures
2. **Exception-based debugging** - Standard output swallowed by Gradle test infrastructure
3. **Verify hypothesis first** - Type arguments implementation was correct but not the blocker
4. **Architecture matters** - Missing binary class resolution was the real blocker (Iteration 6)
5. **ConcurrentHashMap limitations** - Cannot store null values

---

## Document History

- 2026-03-03: Archived from FIXING_ITERATIONS.md and ITERATION_RESULTS.md
