# Java-Direct: Iterations 24-26 Details (Archived)

**Archive Date**: 2026-03-13
**Coverage**: Iterations 24 through 26
**Result**: 1134/1166 → 1150/1167 box tests (97.2% → 98.5%), phased 300/329 → 1374/1442 (95.3%)

> **Warning**: This document is archived for historical reference. Only consult if you need to understand specific implementation decisions or debug regressions.

---

## Iteration Summary

| Iteration | Date | Focus | Tests Fixed | Key Change |
|-----------|------|-------|-------------|------------|
| 24 | 2026-03-12 | Three Regression Fixes | +15 | Constant eval, protected static, sibling inner classes |
| 24b | 2026-03-12 | Cyclic Type Bounds | +6 | StackOverflowError guard in TYPE_PARAMETER_BOUND_FIRST_ROUND |
| 25 | 2026-03-12 | Inherited Inner Classes | +2 | JLS 6.5.2 supertype inner class resolution |
| 25c | 2026-03-12 | Interface Nested Static | +8 | JLS 9.5 implicit static for interface members |
| 26 | 2026-03-12 | Sealed Classes | +9 | `isSealed`, `permittedTypes` implementation |

---

## Iteration 24: Three Regression Fixes

### Fix 1: Constant Evaluation - java.lang Types

**Problem**: `hasConstantNotNullInitializer` checked `classifierQualifiedName == "java.lang.String"`, but java-direct returned just `"String"` for unresolved types.

**Root Cause**: `classifierQualifiedName` in `JavaTypeOverAst.kt` didn't handle the implicit `java.lang.*` import.

**Fix**: Added `JAVA_LANG_TYPES` map in `JavaClassifierTypeOverAst` to resolve common `java.lang` types before falling back to unresolved names.

### Fix 2: Protected Static Visibility

**Problem**: Protected static members returned `Visibilities.Protected` instead of `JavaVisibilities.ProtectedStaticVisibility`.

**Root Cause**: Java has special semantics for protected static — accessible from subclasses in different packages. PSI's `JavaElementUtil.getVisibility()` showed the correct pattern.

**Fix**: Modified `visibility` in `JavaMemberOverAst.kt` to use `ProtectedStaticVisibility` for protected+static, `ProtectedAndPackage` otherwise.

### Fix 3: Sibling Inner Class Resolution

**Problem**: Inner class extending sibling inner class (e.g., `class J { class AImpl {} class A extends AImpl {} }`) — `AImpl` not found.

**Root Cause**: `findLocalClass` didn't check `containingClass.outerClass.findInnerClass(name)`.

**Fix**: Modified `JavaResolutionContext.kt` to search outer class's inner classes.

### Key Learning
- Check reference implementations first (PSI `JavaElementUtil`)
- java.lang implicit import needs handling for `classifierQualifiedName`
- Inner class resolution must walk up the containment hierarchy

---

## Iteration 24b: Cyclic Type Bounds StackOverflowError

### Problem
`testSignatureEnhancementCycleTypeBound` failed with StackOverflowError for cyclic type parameter bounds: `class JavaA<T extends JavaB>` / `class JavaB<T extends JavaA>`.

### Root Cause
In `JavaTypeConversion.kt`, the `classifier == null` branch (used by java-direct) accessed `typeParameterSymbols` during `TYPE_PARAMETER_BOUND_FIRST_ROUND` mode for raw type detection. This triggered class loading of the referenced class, which triggered bounds resolution on that class, creating infinite recursion.

The existing `JavaClass` branch already had a safety guard for this, but the java-direct branch did not.

### Fix
Added `TYPE_PARAMETER_BOUND_FIRST_ROUND` guards in two places in `JavaTypeConversion.kt`:
1. In `toConeTypeProjection` — early return before accessing `typeParameterSymbols`
2. In `toConeKotlinTypeForFlexibleBound` — `takeIf` guard before `typeParameterSymbols`

### Key Learning
- Always check both branches — when one code path has a safety guard, verify parallel paths have equivalent protection
- Cyclic type bounds require careful avoidance of class loading during first-round resolution

---

## Iteration 25: Inherited Inner Class Resolution

### Problem
Test `testInheritedInner` failed: when class B extends class A, references to A's inner class `Inner` from B couldn't be resolved.

### Root Cause
`findLocalClass` only searched within the same compilation unit. Didn't search inner classes of supertypes per JLS 6.5.2.

### Fix
Modified `JavaResolutionContext.kt`:
1. Added `findInnerClassFromSupertypes` — searches inner classes of local supertypes
2. Added outer class supertype search for nested inner classes
3. Fixed same-package supertype resolution with package prefix
4. Walk outer class hierarchy in `resolveSimpleName`

### Key Learning
- Avoid cycles in lazy resolution — use `localClassProvider` or `presentableText` instead of `classifier`
- Same-package types need qualification for callback resolution
- JLS 6.5.2: Inner classes of supertypes AND outer classes' supertypes are in scope

---

## Iteration 25c: Interface Nested Class Static Flag

### Problem
`testInheritedInnerAndNested` failed because java-direct reported `isStatic = false` for classes nested in interfaces.

### Root Cause
Per JLS 9.5, all classes declared as members of an interface are implicitly static. The `isStatic` check only looked at whether the inner class itself was an interface/enum, not whether the outer class was an interface.

### Fix
Modified `JavaClassOverAst.kt`:
```kotlin
// Added: || (outerClass?.isInterface == true)
```
Matches javac-wrapper's `TreeBasedClass.kt:55` implementation.

### Key Learning
- JLS 9.5: all member classes of interfaces are implicitly `public static`
- Check reference implementations — javac-wrapper correctly implements this
- Java has several implicit modifier rules that must be reported to FIR

---

## Iteration 26: Sealed Classes Implementation

### Problem
Java sealed classes not recognized: `isSealed` returned `false`, `permittedTypes` returned empty.

### Root Cause
Initial implementation used `SEALED_KEYWORD` but parser produces just `SEALED` as the token name. Discovered via exception-based AST debugging.

### Fix
Modified `JavaClassOverAst.kt`:
- `isSealed` checks `hasModifier("SEALED")` (not `"SEALED_KEYWORD"`)
- `permittedTypes` finds `PERMITS_LIST` node, extracts `JAVA_CODE_REFERENCE` children

### Investigation Process
1. Parser library check: `SEALED_KEYWORD` defined as constant but not used as token name
2. Exception-based debugging: dumped actual AST structure
3. Discovery: parser produces `SEALED` token, `PERMITS_LIST` works correctly

### Key Learning
- Parser token names != library constant names — always verify via AST dumping
- Exception-based debugging is essential for discovering actual token names
- This was the #1 time-waster: implementing code based on assumed token names

---

## Files Modified

| File | Iterations | Changes |
|------|-----------|---------|
| `JavaTypeOverAst.kt` | 24 | `JAVA_LANG_TYPES` map for implicit java.lang resolution |
| `JavaMemberOverAst.kt` | 24 | `ProtectedStaticVisibility` for protected+static members |
| `JavaResolutionContext.kt` | 24, 25 | Sibling inner class resolution, inherited inner class resolution |
| `JavaTypeConversion.kt` | 24b | Cyclic type bounds StackOverflowError guards |
| `JavaClassOverAst.kt` | 25c, 26 | Interface nested static flag, sealed classes support |

---

## Test Results Progression

| Iteration | Box Tests | Phased Tests | Total Failures |
|-----------|-----------|--------------|----------------|
| Start (post-23) | 1134/1166 (97.2%) | 300/329 (91.2%) | — |
| After 24 | 1149/1167 (98.5%) | — | ~142 |
| After 24b | 1150/1167 (98.5%) | 1323/1442 (91.7%) | ~136 |
| After 25 | 1150/1167 | 1326/1442 | ~133 |
| After 25c | — | — | 134 |
| After 26 | 1150/1167 (98.5%) | 1374/1442 (95.3%) | 125 |

---

*Archived: 2026-03-13*
