# Java-Direct: Iterations 7-16 Details (Archived)

**Archive Date**: 2026-03-06  
**Coverage**: Iterations 7a through 16  
**Result**: 90/138 → 532/601 tests passing

> **Warning**: This document is archived for historical reference. Only consult if you need to understand specific implementation decisions or debug regressions.

---

## Iteration Summary

| Iteration | Date | Focus | Tests Before | Tests After | Key Change |
|-----------|------|-------|--------------|-------------|------------|
| 7a | 2026-03-04 | Array/Vararg Parsing | 90/138 | 96/138 | Array type AST handling |
| 7b | 2026-03-04 | ERROR_ELEMENT Imports | 96/138 | 96/138 | Parser error recovery |
| 7c | 2026-03-04 | Type Parameter Scope | 96/138 | 101/138 | Resolution context + wildcards |
| 8 | 2026-03-04 | Annotations/Nullability | 101/138 | 111/138 | TYPE_USE annotations |
| 9 | 2026-03-04 | Interface Fields/Methods | 111/138 | 115/138 | Implicit modifiers |
| 10 | 2026-03-04 | Nested Interfaces/Enums | 115/138 | 117/138 | Implicit static |
| 11 | 2026-03-05 | External Type Arguments | 117/138 (→601 total) | 128/142 + 331 diag | FIR null classifier branch |
| 12 | 2026-03-05 | Fragmented Star Imports | 128/142 | 128/142 | Parser edge case (superseded) |
| 13 | 2026-03-05 | Annotation Callback | 128/142 | 136/142 | Unified resolution pattern |
| 14 | 2026-03-05 | External Raw Types | 136/142 | 139/142 | ConeRawType in FIR |
| 15 | 2026-03-05 | TYPE_USE Filtering | 139/142 | 139/142 + more diag | Filter non-TYPE_USE |
| 16 | 2026-03-05 | Raw Type Bounds | 139/142 | 532/601 | Type param scope in bounds |

---

## Iteration 7a: Array Types and Vararg Handling

### Problem
Array types (`String[]`) were being parsed as simple types (`String`), causing `NOTHING_TO_OVERRIDE` errors.

### AST Structure Discovered
```
TYPE: String[]
  TYPE: String
    JAVA_CODE_REFERENCE: String
  LBRACKET: [
  RBRACKET: ]
```

### Fix
In `createJavaType()`, detect `LBRACKET` or `ELLIPSIS` child and recursively create `JavaArrayTypeOverAst`.

---

## Iteration 7b: ERROR_ELEMENT Import Handling

### Problem
KMP parser emits `ERROR_ELEMENT` instead of `IMPORT_STATEMENT` for imports starting with reserved words (like `kotlin`).

### Fix
Modified `extractImports()` to process `ERROR_ELEMENT` nodes containing `IMPORT_KEYWORD`, reconstructing FQN from IDENTIFIER children.

---

## Iteration 7c: Type Parameter Scope Resolution

### Problem
Type parameters (`T`, `U`) treated as class names → `MISSING_DEPENDENCY_CLASS: Cannot access class 'T'`.

### Solution
1. Added `typeParametersInScope` map to `JavaResolutionContext`
2. Updated `classifier` to check type parameters first
3. Added wildcard type support (`?`, `? extends`, `? super`)
4. Refactored to unified resolution context pattern

### Key Code
```kotlin
class JavaResolutionContext {
    fun findTypeParameter(name: String): JavaTypeParameter?
    fun withTypeParameters(params: List<JavaTypeParameter>): JavaResolutionContext
}
```

---

## Iteration 8: Annotations and Nullability

### Problem
NPE assertions not generated - `@NotNull` annotations not attached to types.

### Key Finding
TYPE_USE annotations in Java syntax `public @NotNull String foo()` appear in METHOD's MODIFIER_LIST, not on TYPE node.

### Fix
1. Updated `JavaAnnotationOverAst.classId` to resolve via imports
2. Added `extraAnnotations` parameter to type classes
3. Created `createJavaTypeWithAnnotations()` for return types
4. Fixed fragmented import pattern detection

---

## Iteration 9: Interface Fields and Methods

### Problem
Interface fields not accessible, SAM conversion failing.

### Key Finding
Interface members have implicit modifiers:
- Fields: implicitly `public static final`
- Methods without body: implicitly `public abstract`

### Fix
Added override logic in `JavaFieldOverAst.isStatic/isFinal` and `JavaMethodOverAst.isAbstract` to check containing class `isInterface`.

---

## Iteration 10: Nested Interfaces and Enums

### Problem
Nested SAM interface caused IR type parameter mismatch.

### Key Finding
Nested interfaces and enums are implicitly static in Java. This affects FIR's `isInner` determination.

### Fix
Updated `isStatic` to return `true` for nested interfaces/enums:
```kotlin
hasModifier("STATIC_KEYWORD") || (outerClass != null && (isInterface || isEnum))
```

---

## Iteration 11: External Type Arguments Fix

### Problem
Type arguments for external Java types (JDK classes) being dropped → type parameter substitution failures.

### Root Cause
In `JavaTypeConversion.kt`, the `null` classifier branch (for external types) was calling:
```kotlin
classId.constructClassLikeType(emptyArray(), ...)  // BUG: ignoring typeArguments!
```

### Fix
Added full type argument handling to `null` classifier branch - Java→Kotlin mapping, raw type detection, wildcard conversion.

### Impact
Massive improvement: +11 box tests, +67 diagnostic tests.

---

## Iteration 12: Fragmented Star Imports

### Problem
Star imports like `import org.jetbrains.annotations.*;` fragmented across sibling nodes.

### Fix
Enhanced import extraction to skip empty ERROR_ELEMENT nodes and detect `*;` pattern.

**Note**: Annotation handling later superseded by Iteration 13's callback approach.

---

## Iteration 13: Annotation Callback Resolution

### Problem
Hardcoded annotation package lists (from Iteration 12) were incorrect approach.

### Solution
Applied same callback pattern used for type resolution to annotations:
1. Added `isResolved` and `resolveAnnotation(tryResolve)` to `JavaAnnotation` interface
2. FIR calls `resolveAnnotation` with symbolProvider callback

### Key Insight
Annotations should resolve the same way as regular type declarations - check same package, java.lang, and star imports via callback.

---

## Iteration 14: External Raw Types

### Problem
Raw type detection failed for external classes (Kotlin classes, library classes) where `classifier == null`.

### Root Cause
java-direct's `isRaw` only works when `classifier` is `JavaClass`:
```kotlin
override val isRaw: Boolean by lazy {
    !hasParameterList && (classifier as? JavaClass)?.typeParameters?.isNotEmpty() == true
}
```
For external types, `classifier == null`, so `isRaw` always returns `false`.

### Fix
Added raw type detection in FIR's `JavaTypeConversion.kt` that resolves class via symbolProvider and checks for type parameters.

---

## Iteration 15: TYPE_USE Annotation Filtering

### Problem
`@Override` annotation incorrectly appearing on return types.

### Root Cause
`createJavaTypeWithAnnotations()` passes ALL annotations from modifier list, but `@Override` has `@Target(METHOD)`, not TYPE_USE.

### Fix
Added `filterTypeAnnotations()` with hardcoded list of known non-TYPE_USE annotations (`@Override`, `@Deprecated`, etc.). Only filter `extraAnnotations`, not annotations directly on type nodes.

---

## Iteration 16: Raw Type Bounds & Type Parameter Scope

### Problems
1. **Raw Type Bounds**: `UPPER_BOUND_VIOLATED` for raw type bounds in separate files
2. **Type Parameter Scope**: `E` in `<E, S extends Element<E>>` not resolved

### Fixes
1. **FIR**: Always resolve `typeParameterSymbols` for raw type detection, use star projections for upper bounds
2. **java-direct**: Two-phase construction - create type parameter instances first, then update with context containing all siblings

### Key Code
```kotlin
override val typeParameters: List<JavaTypeParameter> by lazy {
    val typeParams = nodes.map { JavaTypeParameterOverAst(it, resolutionContext) }
    val contextWithTypeParams = resolutionContext.withTypeParameters(typeParams)
    typeParams.forEach { it.updateResolutionContext(contextWithTypeParams) }
    typeParams
}
```

---

## Key Learnings Summary

### Debugging
- Exception-based debugging is essential (println doesn't appear in Gradle output)
- Always dump AST structure - don't assume
- Compare with PSI-based implementation when stuck

### Architecture
- Resolution context pattern simplifies scope management
- Callback pattern (`tryResolve: (String) -> Boolean`) cleanly separates Java rules from FIR validation
- Two-phase construction needed when elements reference siblings

### Java Language Rules
- Interface fields: implicitly `public static final`
- Interface methods: implicitly `public abstract` (unless has body)
- Nested interfaces/enums: implicitly static
- TYPE_USE annotations: must filter non-TYPE_USE from modifier list

### FIR Integration
- `null` classifier branch needs same handling as `is JavaClass` branch
- `ConeRawType` wrapper essential for method inheritance semantics
- Flexible types: lower bound with erased args, upper bound with star projections

---

*Archived: 2026-03-06*
