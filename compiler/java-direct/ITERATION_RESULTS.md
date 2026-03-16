# Java-Direct: Iteration Results Log

## Document Purpose

Append-only log of iteration findings, decisions, and learnings.

**Current status**: See `FIXING_ITERATIONS.md` for up-to-date test counts and remaining work.

**Last Updated**: 2026-03-13

---

## Progress Summary

| Phase | Iterations | Box Tests | Phased Tests |
|-------|------------|-----------|--------------|
| Foundation | 1-6 | 90/138 (65.2%) | -- |
| Core Implementation | 7-16 | 139/142 -> 1075/1166 (92.2%) | 242/327 (74.0%) |
| Advanced Features | 17-27 | 1150/1167 (98.5%) | 1374/1442 (95.3%) |

---

## Key Architectural Patterns

### 1. Callback Pattern for Resolution
Used throughout for resolution without FirSession access in Java Model:
- `JavaClassifierType.resolve(tryResolve)` — Type resolution
- `JavaAnnotation.resolveAnnotation(tryResolve)` — Annotation resolution
- `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve)` — Enum class resolution
- `JavaType.filterTypeUseAnnotations(isTypeUse)` — TYPE_USE filtering
- `JavaField.resolveInitializerValue(resolveReference)` — Constant evaluation

### 2. PSI/Java-Direct Discrimination
When shared FIR code needs different behavior:
```kotlin
val isJavaDirectClass = classSource == null && origin.fromSource
```

### 3. Two-Phase Type Parameter Construction
For mutually-referencing type parameters:
1. Create all instances first
2. Update context with all siblings

### 4. Implicit Supertypes
Java classes have implicit inheritance:
- Enums → `java.lang.Enum<E>`
- Annotation types → `java.lang.annotation.Annotation`
- Classes without extends → `java.lang.Object`

---

## Iteration History

### Iterations 1-6: Foundation
**Archive**: `implDocs/archive/ITERATIONS_1_6_DETAILS.md`

Established: Hybrid class finder, import handling, type resolution architecture.

### Iterations 7-16: Core Implementation
**Archive**: `implDocs/archive/ITERATIONS_7_16_DETAILS.md`

Established: Array parsing, type parameter scope, wildcards, annotations, interface modifiers, external type handling, raw types.

### Iterations 17-23: Advanced Features
**Archive**: `implDocs/archive/ITERATIONS_17_23_DETAILS.md`

| Iteration | Focus | Impact |
|-----------|-------|--------|
| 17 | Annotation argument subinterfaces | +4 tests |
| 17b | Annotation method defaults | +16 tests |
| 17c | enumEntriesOrigin fix | No regression |
| 18 | Nested class resolution | +8 tests |
| 19 | TYPE_USE on type arguments | +5 tests |
| 20 | Wildcard parsing, inner class type args | +7 tests |
| 21 | Implicit supertypes | +15 tests |
| 22 | TYPE_USE annotation filtering via callback | +1 test |
| 23 | Cross-language constant evaluation | +4 tests |

### Iterations 24-26: Regression Fixes, Sealed Classes
**Archive**: `implDocs/archive/ITERATIONS_24_26_DETAILS.md`

| Iteration | Focus | Impact |
|-----------|-------|--------|
| 24 | Constant eval, protected static, sibling inner classes | +15 tests |
| 24b | Cyclic type bounds StackOverflowError | +6 tests |
| 25 | Inherited inner class resolution | +2 tests |
| 25c | Interface nested class static flag | +8 tests |
| 26 | Sealed classes (`isSealed`, `permittedTypes`) | +9 tests |

---

## Key Learnings

### What Worked
- **Ad-hoc debugging approach** (iterations 11-16) more effective than detailed upfront planning
- **Callback pattern** for resolution cleanly separates concerns
- **Reference implementation comparison** (javac-wrapper, PSI) often reveals correct approach

### What Didn't Work
- **Detailed upfront plans** overestimated fix counts (same symptom ≠ same cause)
- **Hardcoded lists** for filtering/resolution (use callbacks instead)
- **Modifying shared FIR files** without running PSI regression tests

### Process Improvements
- Debug 2-3 representative tests BEFORE estimating fix count
- Run PSI tests after ANY FIR file modification
- Check javac-wrapper implementation BEFORE implementing new features

---

## Future Iteration Template

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Status
- [ ] In Progress / Completed

### Root Cause Analysis
[Debug 2-3 representative tests first. Document actual cause.]

### Fix
[Solution description and files modified.]

### Test Results
- Box: X/1167, Phased: X/1442

### Key Learnings
[What to add to AGENT_INSTRUCTIONS.md or implDocs/?]

### Investigation State (if stopping mid-work)
- **Current test**: [test being investigated]
- **Hypothesis**: [what you think is causing the failure]
- **Tried**: [approaches attempted and why they failed]
- **Next step**: [what to try next when resuming]
```

---

---

## Iteration 27: Java Records Implementation - 2026-03-12

### Status
⚠️ Partially Complete (Java Model implemented, requires FIR integration)

### Overview
Implemented `recordComponents` property and created `JavaRecordComponentOverAst` class for Java records support (Java 17+).

### Root Cause Analysis
Java records were not recognized by java-direct:
- `isRecord` was already implemented (checks for `RECORD_KEYWORD`)
- `recordComponents` returned empty list
- Record components need to be parsed from `RECORD_HEADER` containing `RECORD_COMPONENT` nodes

### Implementation
1. Created new file `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaRecordComponentOverAst.kt`:
```kotlin
class JavaRecordComponentOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst
) : JavaMemberOverAst(node, containingClass), JavaRecordComponent {
    override val type: JavaType = createJavaType(typeNode, containingClass.memberResolutionContext)
    override val isVararg: Boolean = node.findChildByType("ELLIPSIS") != null
    override val isFromSource: Boolean = true
}
```

2. Modified `JavaClassOverAst.kt` line 169-174:
```kotlin
override val recordComponents: Collection<JavaRecordComponent>
    get() {
        val header = node.findChildByType("RECORD_HEADER") ?: return emptyList()
        return header.getChildrenByType("RECORD_COMPONENT")
            .map { JavaRecordComponentOverAst(it, this) }
    }
```

### Test Results
- Total: 2649 tests
- Before: 125 failures
- After: 125 failures
- **Record tests**: 2/8 passing, 6/8 still failing

### Issue: Requires FIR Integration
The Java Model correctly parses record components, but tests still fail with `UNRESOLVED_REFERENCE: Unresolved reference 'x'`.

**Root cause**: FIR needs to generate **synthetic properties** from record components (similar to how Kotlin creates synthetic properties for Java getters). For example:
- Java: `record MyRecord(int x)`
- Kotlin should see: `mr.x` property (synthetic from `x()` accessor method)

This requires FIR-level changes in files like:
- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` — Synthetic property generation
- `compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt` — Record component handling

### Key Learnings
1. **Java Model != Full Feature Support** — Parsing record components is not enough; FIR must generate synthetic properties
2. **Records are more complex than sealed classes** — Sealed classes only need metadata, records need synthetic member generation
3. **Partial implementation is valuable** — Even though tests don't pass, the foundation is in place for future FIR integration
4. **Similar to getters** — Record component handling should follow the pattern of synthetic properties from Java getters/setters

---

## Iteration 28: Java Records FIR Integration - 2026-03-13

### Status
✅ Complete — all 6 phased record tests fixed

### Root Cause Analysis
Three bugs, all preventing `createDeclarationsForJavaRecord` from working:

1. **`isRecord` token name mismatch** (primary bug): `RECORD_KEYWORD.toString()` returns `"RECORD"` not `"RECORD_KEYWORD"` (unlike `ENUM_KEYWORD` which returns `"ENUM_KEYWORD"`). So `findChildByType("RECORD_KEYWORD")` always returned null → `isRecord = false` → `createDeclarationsForJavaRecord` never called.

2. **`isVararg` incomplete check**: `JavaRecordComponentOverAst.isVararg` only checked the component node directly, not inside the TYPE child (where ELLIPSIS can live). Caused vararg record component to be treated as array parameter.

3. **Canonical constructor not detected as primary**: `isPrimary` in `convertJavaConstructorToFir` uses PSI-based `JavaPsiRecordUtil.isCanonicalConstructor`. For java-direct (no PSI), all constructors got `isPrimary = false`, causing `createDeclarationsForJavaRecord` to always add a synthetic primary, creating duplicates for explicit canonical constructors → `OVERLOAD_RESOLUTION_AMBIGUITY`.

### Fix
1. `JavaClassOverAst.kt`: `findChildByType("RECORD_KEYWORD")` → `findChildByType("RECORD")`
2. `JavaRecordComponentOverAst.kt`: `isVararg` now also checks inside TYPE node (matching `JavaValueParameterOverAst` pattern)
3. `FirJavaFacade.kt`: Added third condition to `isPrimary`: for source-based (non-PSI) records, compare constructor parameter names with record component names in order (JLS guarantees same names for explicit canonical constructors)

### Test Results
- Box: 1152/1167 (was 1150, +2)
- Phased: 1347/1442 (was 1341 on same machine/run, +6)
- Record tests: 6/6 passing (was 0/6)

### Key Learnings
- **RECORD_KEYWORD token mismatch**: Like SEALED vs SEALED_KEYWORD, RECORD_KEYWORD's toString is "RECORD". Always verify token toString via sources/debugging.
- **Canonical constructor detection**: Java spec guarantees explicit canonical constructors have same parameter names as record components — name comparison is reliable.
- **FirJavaFacade.isPrimary TODO**: The `// TODO get rid of dependency on PSI KT-63046` comment marks this exact problem. The fix aligns with that future direction.

---

## Iteration 29: Ambiguity Detection for Inner Classes - 2026-03-13

### Status
✅ Complete — 2 tests fixed, 1 architectural limitation documented

### Root Cause Analysis
When multiple Java supertypes have inner classes with the same name, javac reports ambiguity at the method declaration site. Verified with javac:

```java
class X { public class Z {} }
interface I { public class Z {} }
class Y extends X implements I {
    public Z getZ() { return null; }  // javac error: reference to Z is ambiguous
}
```

Java-direct was not detecting this ambiguity, allowing one of the `Z` classes to be resolved arbitrarily.

**Debug process**: 
1. Confirmed javac behavior for 3 test scenarios
2. Traced through `findInnerClassFromSupertypes` — returned first match instead of checking all
3. Traced through `resolveFromSupertypes` — same issue in callback-based resolution

### Implementation

Modified `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt`:

**1. `findInnerClassFromSupertypes()` (lines 71-106)**
- Changed from returning first match to collecting all matches in `Set<JavaClass>`
- Returns `null` if `allFound.size > 1` (ambiguity detected)
- Works for classes within same compilation unit

**2. `resolveFromSupertypes()` + `resolveFromSupertypesRecursive()` (lines 237-286)**
- Changed from returning first match to collecting all matches in `Set<String>` (FQNs)
- Returns `null` if `allMatches.size > 1` (ambiguity detected)
- Used for callback-based resolution by FIR

### Test Results
- Box: 1152/1167 (15 failing) — no change
- Phased: 1349/1442 (93 failing) — **+2 improvement** (was 95)
- **Total: ~2501/2609 passing (~108 failing, down from ~110)**

**Fixed tests:**
- ✅ `testInheritanceAmbiguity` — Direct conflict: `x.Z` vs `i.Z` in same package
- ✅ `testInheritanceAmbiguity3` — Direct conflict: `i.Z` vs `i2.Z` both as direct supertypes

---

## Iteration 30: Cross-file Ambiguity Detection

**Goal**: Implement cross-file ambiguity detection for `testInheritanceAmbiguity2` with minimal eager parsing

**Approach**: Design document created at `implDocs/CROSS_FILE_AMBIGUITY_SOLUTION.md` proposing 4-phase minimal solution:
1. Add `classFinderProvider` callback to `JavaResolutionContext`
2. Implement `getDirectSupertypes()` - lightweight parsing of extends/implements clauses
3. Implement `collectInheritedInnerClasses()` - recursive collection with cycle detection
4. Update ambiguity detection to use cross-file detection as fallback

**Implementation**:
- ✅ Added `classFinderProvider` parameter to `JavaResolutionContext` constructor and threaded through all context creation methods
- ✅ Implemented `getDirectSupertypes()` with caching in `JavaClassFinderOverAstImpl`
- ✅ Implemented `collectInheritedInnerClasses()` with cycle detection
- ✅ Updated `findInnerClassFromSupertypes()` and `resolveFromSupertypes()` to use cross-file detection when local resolution finds nothing
- ✅ Added helper methods: `findFilesForClass()`, `resolveSupertypeReference()`, `getInnerClassNames()`

**Errors encountered and fixed**:
1. Type mismatch using `relativeClassName.child()` - fixed by using `createNestedClassId()`
2. Test timeout due to incorrect `ClassId.topLevel(fqName)` usage - fixed by adding `fqNameToClassId()` helper
3. Lowercase class name bug in `fqNameToClassId()`:
   - Initial implementation used heuristic assuming class names start with uppercase
   - Test case has lowercase class names (x, y, i, i2)
   - For `FqName("a.y")`, incorrectly returned `ClassId(FqName.ROOT, FqName("a.y"))` instead of `ClassId(FqName("a"), FqName("y"))`
   - Fixed by using `packageFqName` directly from context instead of heuristic

**Test results**: **ALL 4 ambiguity tests pass!** ✅
- ✅ `testInheritanceAmbiguity`
- ✅ `testInheritanceAmbiguity2` - **FIXED!**
- ✅ `testInheritanceAmbiguity3`
- ✅ `testInheritanceAmbiguity4`

**Files modified**:
- `JavaResolutionContext.kt` - added infrastructure, cross-file detection logic, and `fqNameToClassId()` fix
- `JavaClassFinderOverAstImpl.kt` - added supertype parsing and inner class collection
- `InheritanceAmbiguity2.kt` - updated test expectations (removed `MISSING_DEPENDENCY_CLASS` error)

### Key Learnings
1. **Set-based collection** handles duplicate paths naturally (same class found via different supertype chains)
2. **Two-level ambiguity detection needed**: both Java Model level (`findInnerClassFromSupertypes`) and FIR callback level (`resolveFromSupertypes`)
3. **Cross-file detection is possible** with minimal eager parsing - the "architectural limitation" claim was incorrect
4. **Avoid heuristics when exact information is available** - using `packageFqName` from context is more reliable than uppercase detection
5. **Test with edge cases** - lowercase class names revealed the heuristic bug

---

*For detailed iteration histories, see `implDocs/archive/`*
