# Java-Direct: Iterations 17-23 Details (Archived)

**Archive Date**: 2026-03-12  
**Coverage**: Iterations 17 through 23  
**Result**: 1075/1166 → 1134/1166 box tests (92.2% → 97.2%)

> **Warning**: This document is archived for historical reference. Only consult if you need to understand specific implementation decisions or debug regressions.

---

## Iteration Summary

| Iteration | Date | Focus | Box Tests After | Key Change |
|-----------|------|-------|-----------------|------------|
| 17 | 2026-03-06 | Annotation Arguments | 1076/1166 | Annotation argument subinterfaces |
| 17b | 2026-03-06 | Annotation Method Defaults | 1092/1166 | Default value parsing, enum constants |
| 17c | 2026-03-11 | enumEntriesOrigin Fix | No regression | PSI/java-direct discrimination |
| 18 | 2026-03-07 | Nested Class Resolution | 1100/1166 | Callback-based nested class lookup |
| 19 | 2026-03-07 | TYPE_USE on Type Args | 1105/1166 | Annotations on generic type arguments |
| 20 | 2026-03-10 | Wildcard/Inner Class | 1114/1166 | Wildcard parsing, inner class type args |
| 21 | 2026-03-10 | Implicit Supertypes | 1129/1166 | Enum/Annotation/Object implicit supertypes |
| 22 | 2026-03-10 | TYPE_USE Filtering | 1130/1166 | Callback-based annotation filtering |
| 23 | 2026-03-11 | Cross-Language Constants | 1134/1166 | FIR callback for constant evaluation |

---

## Iteration 17: Annotation Argument Subinterfaces

### Problem
`JavaAnnotationArgumentOverAst` only implemented base interface with `name`, not value subinterfaces.

### Fix
Implemented all annotation argument subinterfaces:
- `JavaLiteralAnnotationArgumentOverAst` — String, char, boolean, numeric literals
- `JavaArrayAnnotationArgumentOverAst` — Array initializers `{a, b, c}`
- `JavaEnumValueAnnotationArgumentOverAst` — Enum constant references
- `JavaClassObjectAnnotationArgumentOverAst` — Class literals `Foo.class`
- `JavaAnnotationAsAnnotationArgumentOverAst` — Nested annotations

### Key Learning
FIR expects specific subinterfaces to extract values. Implementing only `JavaAnnotationArgument` causes null value errors.

---

## Iteration 17b: Annotation Method Default Values

### Problem
Annotation methods with `default` values not parsed. `ANNOTATION_METHOD` node type different from `METHOD`.

### Fix
1. Added `ANNOTATION_METHOD` to method discovery
2. Implemented `annotationParameterDefaultValue` to find value after `DEFAULT_KEYWORD`
3. Added `ENUM_CONSTANT` to fields getter
4. Created `JavaClassifierTypeForEnumEntry` for enum constant types

### Key Learning
Annotation interface methods use `ANNOTATION_METHOD` node type, not `METHOD`. Enum constants are `ENUM_CONSTANT` nodes.

---

## Iteration 17c: enumEntriesOrigin Fix

### Problem
Change to use `FirDeclarationOrigin.Java.Source/Library` for enum entries broke PSI-based tests.

### Root Cause
`FirDeclarationOrigin.Source` requires a source element (PSI). Java-direct classes don't have PSI.

### Fix
Discriminate java-direct from PSI by checking `classSource == null`:
```kotlin
val enumEntriesOrigin = when {
    classSource == null && firJavaClass.origin.fromSource -> FirDeclarationOrigin.Java.Source
    firJavaClass.origin.fromSource -> FirDeclarationOrigin.Source
    else -> FirDeclarationOrigin.Library
}
```

### Key Learning
When modifying shared FIR code, always verify PSI tests don't regress.

---

## Iteration 18: Nested Class Resolution

### Problem
`Map.Entry`, `Outer.Inner` not resolved when outer class is in binary.

### Fix
Added callback-based resolution in `JavaResolutionContext.resolveNestedClass()`:
1. Resolve outer class first via same package, java.lang, star imports
2. Append nested class name

In FIR `JavaTypeConversion.kt`, added `findClassId()` that probes different package/class splits.

### Key Learning
Nested class resolution requires probing the symbol provider with different ClassId constructions.

---

## Iteration 19: TYPE_USE on Type Arguments

### Problem
`List<@NotNull Integer>` — annotations on type arguments not parsed.

### Fix
In `createJavaType()`, extract `ANNOTATION` children from TYPE nodes and pass as `extraAnnotations`.

### Key Learning
TYPE_USE annotations on type arguments appear as siblings to `JAVA_CODE_REFERENCE` under TYPE node.

---

## Iteration 20: Wildcard Parsing + Inner Class Type Args

### Problems
1. Wildcards parsed as classifier types (checked nested TYPE before QUEST)
2. Non-static inner classes missing implicit outer type arguments

### Fixes
1. Check for QUEST on input TYPE node BEFORE looking for nested TYPE
2. Added `JavaTypeParameterTypeOverAst` for implicit type arguments from enclosing classes

### Key Learning
AST node order matters. Non-static inner classes inherit type parameters from enclosing classes.

---

## Iteration 21: Implicit Supertypes

### Problem
Enums missing `java.lang.Enum<E>` supertype caused `ordinal` property not found.

### Fix
Added implicit supertypes in `JavaClassOverAst.supertypes`:
- Enums: `java.lang.Enum<E>` (with self-referential type argument)
- Annotation types: `java.lang.annotation.Annotation`
- Classes without extends: `java.lang.Object`

### Key Learning
Java has implicit inheritance rules critical for correct compilation.

---

## Iteration 22: TYPE_USE Annotation Filtering

### Problem
Non-TYPE_USE annotations (like `@Override`) incorrectly appearing on return types.

### Failed Approaches
1. Hardcoded blocklist — rejected as unmaintainable
2. FIR-level filtering — broke PSI tests (double filtering)

### Successful Approach
Callback pattern: `filterTypeUseAnnotations(isTypeUse: (classId) -> Boolean)`
- Default returns annotations unchanged (javac-wrapper already filters)
- Java-direct overrides to use FIR callback

### Key Learning
Callback pattern allows each implementation to control its own behavior without affecting others.

---

## Iteration 23: Cross-Language Constant Evaluation

### Problem
Java field initializers referencing Kotlin constants (`MainKt.FOO + 1`) not evaluated.

### Fix
Added `resolveInitializerValue(resolveReference: (classQualifier, fieldName) -> Any?)` callback:
1. Java-direct's `ConstantEvaluator` uses callback for external references
2. FIR's `lazyInitializer` provides callback that resolves via `symbolProvider`

### Key Learning
Deferred resolution via FIR's `lazyInitializer` timing ensures Kotlin symbols are already resolved.

---

## Key Architecture Patterns

### Callback Pattern for Resolution
Used throughout iterations 17-23:
- Type resolution: `resolve(tryResolve: (String) -> Boolean)`
- Annotation resolution: `resolveAnnotation(tryResolve)`
- Enum class resolution: `resolveEnumClass(tryResolve)`
- Type annotation filtering: `filterTypeUseAnnotations(isTypeUse)`
- Constant evaluation: `resolveInitializerValue(resolveReference)`

### PSI/Java-Direct Discrimination
When modifying shared FIR code:
```kotlin
val isJavaDirectClass = classSource == null && origin.fromSource
```

### Two-Phase Construction
For elements that reference siblings (type parameters):
1. Create all instances first
2. Update each with context containing all siblings

---

## Files Modified

| File | Changes |
|------|---------|
| `JavaAnnotationOverAst.kt` | Annotation argument subinterfaces, enum resolution callback |
| `JavaMemberOverAst.kt` | Annotation defaults, enum constants, `resolveInitializerValue` |
| `JavaTypeOverAst.kt` | Wildcard parsing, inner class type args, TYPE_USE filtering |
| `JavaClassOverAst.kt` | Implicit supertypes, annotation methods |
| `JavaResolutionContext.kt` | Nested class resolution, supertype resolution |
| `ConstantEvaluator.kt` | External reference callback |
| `FirJavaFacade.kt` | enumEntriesOrigin discrimination, constant resolution |
| `JavaTypeConversion.kt` | `findClassId`, TYPE_USE filtering callback |
| `javaAnnotationsMapping.kt` | Enum class resolution callback |
| `javaElements.kt` | `JavaField.resolveInitializerValue` interface |
| `annotationArguments.kt` | `JavaEnumValueAnnotationArgument.resolveEnumClass` interface |

---

## Test Results Progression

| Iteration | Box Tests | Phased Tests |
|-----------|-----------|--------------|
| Start | 1075/1166 (92.2%) | 242/327 (74.0%) |
| After 17 | 1076/1166 | 245/327 |
| After 17b | 1092/1166 | — |
| After 18-19 | 1105/1166 | — |
| After 20 | 1114/1166 | — |
| After 21 | 1129/1166 | — |
| After 22 | 1130/1166 | 300/329 |
| After 23 | 1134/1166 (97.2%) | 300/329 (91.2%) |

---

*Archived: 2026-03-12*
