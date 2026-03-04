# Java-Direct: Iteration Results Log

## Document Purpose

This file captures key findings, decisions, and learnings from each iteration. It serves as:
1. **Progress tracker**: What's been completed
2. **Knowledge base**: Discoveries about the codebase and architecture
3. **Context updater**: Input for updating AGENT_INSTRUCTIONS.md after multiple iterations

**Usage**: After completing each iteration, the agent MUST append a results section below.

**Last Updated**: 2026-03-04

---

## Iterations 1-6: Completed (Archived)

**Status**: ✅ All completed  
**Final Result**: 90/138 (65.2%) box tests passing  
**Archive**: See `implDocs/archive/ITERATIONS_1_6_DETAILS.md` for full details

### Progress Summary

| Iteration | Date | Focus | Tests Before | Tests After | Key Change |
|-----------|------|-------|--------------|-------------|------------|
| 1 | 2026-02-23 | Constructor Analysis | 0/138 | 1/138 | Fixed `hasDefaultConstructor()` |
| 2 | 2026-02-24 | Type Resolution Architecture | 1/138 | 1/138 | Verified classifierQualifiedName approach |
| 3 | 2026-02-24 | Import Handling | 1/138 | 11/138 | Implemented JavaImports |
| 4 | 2026-02-25 | Star Imports + Parameters | 11/138 | 30/138 | Callback resolution + parameter parsing |
| 5 | 2026-02-27 | Type Arguments | 30/138 | 31/138 | Generic type arguments + visibility fix |
| 6 | 2026-03-03 | Hybrid JavaClassFinder | 31/138 | 90/138 | Combined source+binary class finding |

### Key Architectural Decisions

1. **Type Resolution in FIR Layer** (Iteration 2): Java Model provides names, FIR provides resolution. No `FirSession` access in Java Model.

2. **Callback Pattern for Star Imports** (Iteration 4): `resolve(tryResolve: (String) -> Boolean)` allows Java Model to implement Java resolution rules while FIR validates existence.

3. **Hybrid Finder Architecture** (Iteration 6): Source-first, binary-fallback via `CombinedJavaClassFinder`. Added `defaultFinderProvider` parameter to `JavaClassFinderFactory`.

### Key Files Modified (Iterations 1-6)

- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` - Java class model
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` - Type representations
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` - Methods, fields, parameters
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaImports.kt` - Import handling (NEW)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaDirectComponentRegistrar.kt` - Factory with hybrid finder
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` - Source class finder
- `compiler/cli/src/org/jetbrains/kotlin/cli/jvm/compiler/extensions/JavaClassFinderFactory.kt` - Added defaultFinderProvider
- `compiler/cli/src/org/jetbrains/kotlin/cli/jvm/compiler/VfsBasedProjectEnvironment.kt` - Passes defaultFinderProvider
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt` - Added isResolved/resolve()
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` - Uses resolve callback

### Remaining Failures (48 tests)

After Iteration 6, 48 tests still fail. Likely causes:
1. **Type parameter handling**: `T`, `U`, `S` being treated as class names
2. **Generics/wildcards**: Complex generic signatures (`? extends`, `? super`)
3. **SAM lambda inference**: `it` parameter not resolved
4. **Other semantic issues**: Not related to class finding

---

## Template for Future Iterations

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Status
- [ ] In Progress / ✅ Completed

### Summary
[2-3 sentences describing what was done and the result]

### Key Findings
[Bullet points of important discoveries]

### Changes Made
[List of files and what changed]

### Test Results
- Unit tests: X/Y passing
- Box tests: X/138 passing (X%) - UP/DOWN from X/138 (X%)

### Issues Encountered
[Problems hit and how they were solved]

### Key Learnings
[What should be remembered for future work]
```

---

## Future Iterations Start Below

<!-- Add new iteration results here, newest at top -->

## Iteration 8: Annotations and Nullability - 2026-03-04

### Status
- ✅ Completed

### Summary
Implemented annotation parsing for nullability annotations (`@NotNull`, `@Nullable`) to enable FIR to generate proper null-checks. Fixed `JavaAnnotationOverAst.classId` to resolve annotation class names via imports, added annotation extraction from modifier lists for members and parameters, and implemented TYPE_USE annotation support for return types. Also fixed fragmented import patterns where the KMP parser splits imports across sibling nodes. Improved from 101/138 (73.2%) to 111/138 (80.4%) - gained 10 tests.

### Key Findings

1. **TYPE_USE Annotations in Method Modifier List**: In Java syntax `public @NotNull String nullString()`, the `@NotNull` annotation appears in the METHOD's `MODIFIER_LIST`, not on the TYPE node. The AST structure is:
   ```
   METHOD:
     MODIFIER_LIST: public @NotNull
       PUBLIC_KEYWORD: public
       ANNOTATION: @NotNull
     TYPE: String
       JAVA_CODE_REFERENCE: String
   ```
   FIR expects these annotations on `JavaType.annotations`, so we pass them when creating the return type.

2. **Annotation ClassId Resolution**: `JavaAnnotationOverAst.classId` must resolve the simple annotation name (e.g., `NotNull`) to its fully qualified name (e.g., `org.jetbrains.annotations.NotNull`) using imports. Without this, FIR reports `MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR`.

3. **FIR's Annotation Handling**: FIR converts Java annotations in `javaAnnotationsMapping.kt`:
   - `JavaAnnotation.toFirAnnotation()` uses `classId` to build the type
   - `JavaType.annotations` are converted via `convertAnnotationsToFir()`
   - Nullability qualifiers are extracted in `FirAnnotationTypeQualifierResolver`

4. **Fragmented Import Pattern**: When the KMP parser receives malformed input (content with leading comments/newlines), it splits imports across sibling nodes:
   ```
   CLASS:
     IMPORT_LIST: (empty)
     ERROR_ELEMENT: import       <- IMPORT_KEYWORD here
       IMPORT_KEYWORD: import
     MODIFIER_LIST:
     TYPE: org.jetbrains.annotations.NotNull  <- FQN here!
     SEMICOLON: ;
   ```
   The fix scans for `ERROR_ELEMENT(IMPORT_KEYWORD)` followed by `TYPE` siblings, skipping whitespace and modifier lists.

### Changes Made

| File | Change |
|------|--------|
| `JavaAnnotationOverAst.kt` | Updated constructor to accept `JavaResolutionContext`. `classId` now resolves via `resolutionContext.getSimpleImport()`. |
| `JavaTypeOverAst.kt` | Added `extraAnnotations` parameter to all type classes. Created `createJavaTypeWithAnnotations()` to pass TYPE_USE annotations from modifier list to return type. |
| `JavaMemberOverAst.kt` | `annotations` property now parses from MODIFIER_LIST. `JavaMethodOverAst.returnType` uses `createJavaTypeWithAnnotations()` to include TYPE_USE annotations. `JavaValueParameterOverAst.annotations` implemented. |
| `JavaClassOverAst.kt` | `annotations` property passes `resolutionContext` to `JavaAnnotationOverAst`. |
| `JavaResolutionContext.kt` | Added fragmented import pattern detection in `extractImports()` - scans for `ERROR_ELEMENT(IMPORT_KEYWORD)` followed by `TYPE` sibling. |

### Test Results
- Box tests: 111/138 passing (80.4%) - UP from 101/138 (73.2%)
- Gained: 10 tests (+7.2%)

### Tests Fixed
| Test | Issue Fixed |
|------|-------------|
| `testInFunctionWithExpressionBody` | NPE assertion now works - `@NotNull` annotation resolved |
| `testInMemberPropertyInitializer` | NPE assertion for member property |
| `testInPropertyGetterWithExpressionBody` | NPE assertion for property getter |
| `testInTopLevelPropertyInitializer` | NPE assertion for top-level property |
| `testNnStringVsTXArray` | Nullability annotation on array type |
| `testNnStringVsTXString` | Nullability annotation on String type |
| `testAddedOverloadWithAtomics` | Annotation resolution fixed import handling |
| + 3 others | Various annotation-related fixes |

### Remaining Failures (27 tests)

| Category | Count | Example Tests |
|----------|-------|---------------|
| OTHER | 6 | testKjkWithRawTypes, testKt43217, testRawTypeArgumentInJavaSuperType |
| ARGUMENT_TYPE_MISMATCH | 4 | testFunctionWithBigArity, testGenericSamSmartcast |
| CANNOT_INFER_PARAMETER_TYPE | 4 | testFunctionAssertion, testLocalEntities, testSamTypeParameter |
| UNRESOLVED_REFERENCE | 3 | testGenericSamProjectedOut, testJavaInterfaceFieldDirectAccess |
| NONE_APPLICABLE | 3 | testIntersectionKotlinJavaAtomics, testKotlinToJavaHierarchy |
| MISSING_DEPENDENCY_CLASS | 3 | testJavaToKotlinHierarchy, atomics tests |
| AbstractMethodError | 2 | testDelegationToJavaDnn, testPrimitiveSubstitutionToDnnParameter |
| NOTHING_TO_OVERRIDE | 1 | testOverrideWithGenericArrayParameterType |
| NoSuchMethodError | 1 | testInheritanceWithWildcard |

### Architecture Decisions

**TYPE_USE Annotation Propagation**: Rather than modifying how types are created everywhere, we:
1. Added `extraAnnotations` parameter to all `JavaTypeOverAst` subclasses
2. Created `createJavaTypeWithAnnotations()` helper that extracts annotations from modifier list
3. Only `JavaMethodOverAst.returnType` and `JavaFieldOverAst.type` need to use this helper

**Fragmented Import Recovery**: Parser edge case handling in `extractImports()`:
```kotlin
// Pattern: ERROR_ELEMENT(IMPORT_KEYWORD) → skip whitespace/modifiers → TYPE
if (node.type == "ERROR_ELEMENT" && node.findChildByType("IMPORT_KEYWORD") != null) {
    for (sibling in siblings) {
        if (sibling.type in ["WHITE_SPACE", "MODIFIER_LIST"]) continue
        if (sibling.type == "TYPE") {
            val fqName = sibling.findChildByType("JAVA_CODE_REFERENCE").text
            simpleImports[fqName.substringAfterLast('.')] = FqName(fqName)
        }
        break
    }
}
```

### Key Learnings

1. **TYPE_USE Annotations in Java Syntax**: Annotations before return type (`public @NotNull String`) are syntactically in the method modifier list but semantically belong to the return type. PSI handles this transparently; java-direct needs explicit handling.

2. **Parser Edge Cases**: When the KMP parser receives unexpected input (multi-file content with comments), it may fragment constructs across sibling nodes. Robust recovery requires pattern matching on the AST structure.

3. **Annotation Resolution Path**: `JavaAnnotation.classId` → FIR lookup → type enhancement → null-check generation. Any break in this chain causes either compile errors or missing runtime checks.

4. **ExtraAnnotations Pattern**: Adding an optional `extraAnnotations` parameter to type constructors is cleaner than wrapper classes (which FIR doesn't recognize) or modifying all call sites.

---

## Iteration 7c: Type Parameter Scope Resolution - 2026-03-04

### Status
- ✅ Completed

### Summary
Implemented type parameter scope resolution to fix `MISSING_DEPENDENCY_CLASS: Cannot access class 'T'` errors. Added type parameter tracking to `JavaResolutionContext`, updated classifier resolution to check type parameters first, and added wildcard type support. Also refactored from separate `localScope`/`imports` threading to unified `resolutionContext` pattern. Improved from 96/138 (69.6%) to 101/138 (73.2%) - gained 5 tests.

### Key Findings

1. **Type Parameters Treated as Classes**: When java-direct parsed `class Foo<T> { T getValue(); }`, the `T` in return type was being resolved via `classifierQualifiedName` as a class name, causing FIR to emit `MISSING_DEPENDENCY_CLASS: Cannot access class 'T'`.

2. **PSI/Javac Handle This Correctly**: Both PSI-based (`JavaClassifierImpl.java:32-38`) and javac-based (`ClassifierResolver.kt:199-205`) implementations check if a name refers to a type parameter before treating it as a class.

3. **Resolution Context Pattern**: Replaced messy threading of `localScope` and `imports` parameters with a `JavaResolutionContext` class that encapsulates all resolution data. This follows FIR's scope pattern.

4. **Wildcard AST Structure**: Wildcards use `QUEST` node with optional `EXTENDS_KEYWORD` or `SUPER_KEYWORD`:
   ```
   TYPE: ? extends T
     QUEST: ?
     EXTENDS_KEYWORD: extends
     TYPE: T
       JAVA_CODE_REFERENCE: T
   ```

5. **Inner Class Resolution**: Members need to resolve inner classes by simple name (e.g., `X` instead of `Outer.X`). Added `containingClassProvider` to resolution context.

### Changes Made

| File | Change |
|------|--------|
| `JavaResolutionContext.kt` | Added `typeParametersInScope` map, `findTypeParameter()`, `withTypeParameters()`, `withContainingClass()` methods. Refactored to be the central resolution data holder. |
| `JavaTypeOverAst.kt` | Updated `classifier` to check type parameters first. Updated `classifierQualifiedName` and `isResolved` for type parameter handling. Added wildcard detection in `createJavaType()`. |
| `JavaClassOverAst.kt` | Added `memberResolutionContext` that includes class type parameters and containing class reference. Updated supertypes and inner class creation to use member context. |
| `JavaMemberOverAst.kt` | Changed `resolutionContext` to use `containingClass.memberResolutionContext`. `JavaMethodOverAst` and `JavaConstructorOverAst` now add their own type parameters via `withTypeParameters()`. |
| `JavaImports.kt` | DELETED - functionality merged into `JavaResolutionContext` |
| `LocalJavaScope.kt` | DELETED - functionality merged into `JavaResolutionContext` |

### Test Results
- Box tests: 101/138 passing (73.2%) - UP from 96/138 (69.6%)
- Gained: 5 tests (+3.6%)

### Tests Fixed
- Tests using type parameters in method signatures (`<T> T getValue()`)
- Tests using type parameters in field types (`T field`)
- Tests using class type parameters in supertypes (`class Foo<T> extends Bar<T>`)
- Tests using type parameter bounds (`<T extends Number>`)

### Remaining Failures (37 tests)
| Category | Count | Example Tests |
|----------|-------|---------------|
| Kotlin classes from Java | ~12 | testLambdaInstanceOf, testKotlinToJavaHierarchy |
| Complex inheritance | ~8 | testInheritanceWithWildcard, testKjkWithRawTypes |
| Nullability assertions | ~8 | testInFunctionWithExpressionBody, testLocalEntities |
| Other generic issues | ~9 | testGenericSamSmartcast, testRawTypeArgumentInJavaSuperType |

### Architecture Changes

**Before (messy parameter threading):**
```kotlin
class JavaClassOverAst(node, source, outerClass, localScope, imports)
class JavaMethodOverAst(node, containingClass)  // had to cast to get localScope
fun createJavaType(node, source, localScope, imports)
```

**After (unified resolution context):**
```kotlin
class JavaClassOverAst(node, resolutionContext, outerClass)
class JavaMethodOverAst(node, containingClass)  // uses containingClass.memberResolutionContext
fun createJavaType(node, resolutionContext)
```

### Key Learnings

1. **Type Parameter Scope is Hierarchical**: Class type params → Method type params → Type bounds. Each level can shadow outer scopes.

2. **Inner vs Static Nested Classes**: Non-static inner classes inherit outer class type parameters, static nested classes don't. Must check STATIC_KEYWORD modifier.

3. **Resolution Context Pattern**: Consolidating resolution data into a single context object greatly simplifies the API and reduces errors from mismatched parameters.

4. **Wildcard isExtends Semantics**: `isExtends=true` for unbounded `?` and `? extends X`, `isExtends=false` only for `? super X`.

5. **Lazy Initialization Needed**: `memberResolutionContext` must be lazy to avoid circular initialization when type parameters reference each other in bounds.

---

## Iteration 7b: ERROR_ELEMENT Import Handling - 2026-03-04

### Status
- ✅ Completed

### Summary
Fixed import resolution for Java imports starting with reserved words (like `kotlin`). The KMP Java parser emits `ERROR_ELEMENT` instead of `IMPORT_STATEMENT` for such imports, causing `MISSING_DEPENDENCY_CLASS` errors. Added ERROR_ELEMENT handling to `extractImports()` in `JavaResolutionContext.kt`.

### Key Findings

1. **KMP Parser treats `import kotlin.*` as parse error**: The KMP Java parser (used by java-direct) incorrectly parses imports starting with 'kotlin' (a reserved word in Java 9+ module system) as `ERROR_ELEMENT` instead of `IMPORT_STATEMENT`.

2. **AST Structure for ERROR_ELEMENT imports**:
   ```
   IMPORT_LIST:
     ERROR_ELEMENT: import kotlin.jvm.functions.FunctionN;
       IMPORT_KEYWORD: import
       IDENTIFIER: kotlin
       DOT: .
       IDENTIFIER: jvm
       DOT: .
       IDENTIFIER: functions
       DOT: .
       IDENTIFIER: FunctionN
       SEMICOLON: ;
   ```
   Compare to normal import:
   ```
   IMPORT_STATEMENT: import java.util.List;
     IMPORT_KEYWORD: import
     JAVA_CODE_REFERENCE: java.util.List
       ...
     SEMICOLON: ;
   ```

3. **Impact on type resolution**: Without this fix, `FunctionN`, `Function0`, etc. from `kotlin.jvm.functions` were unresolvable, causing `MISSING_DEPENDENCY_CLASS: Cannot access class 'FunctionN'` errors.

### Changes Made
| File | Change |
|------|--------|
| `JavaResolutionContext.kt` | Added ERROR_ELEMENT handling in `extractImports()` - checks for ERROR_ELEMENT nodes containing IMPORT_KEYWORD and reconstructs FQN from IDENTIFIER children |

### Test Results
- Box tests: 96/138 passing (69.6%) - same count but different error profile
- **Key change**: `testFunctionWithBigArity` now fails with `ARGUMENT_TYPE_MISMATCH` instead of `MISSING_DEPENDENCY_CLASS` - proving import resolution works

### Error Profile Change
| Before Fix | After Fix |
|------------|-----------|
| `MISSING_DEPENDENCY_CLASS: Cannot access class 'FunctionN'` | `ARGUMENT_TYPE_MISMATCH: actual type is 'FunctionN<Any>', but 'FunctionN<!>{1}' was expected` |

This shows the fix works - `FunctionN` is now being resolved, but there are separate type argument handling issues.

### Remaining Failures Categorization (42 tests)

| Category | Count | Tests |
|----------|-------|-------|
| MISSING_DEPENDENCY_CLASS | 13 | testGenericSamSmartcast, testInheritanceWithWildcard, testJavaToKotlinHierarchy, testKjkWithRawTypes, testKt42824, testKt42825, testLocalEntities, testNoAssertionForNulllableCaptured, testOverrideWithGenericArrayParameterType, testPropertyVarianceConflict, testSamUnboundTypeParameter, testUsingJavaAtomicWhenKotlinAtomicExpected, testUsingKotlinAtomicWhenJavaAtomicExpected |
| NPE_ASSERTION | 8 | testInFunctionWithExpressionBody, testInLocalFunctionWithExpressionBody, testInLocalVariableInitializer, testInMemberPropertyInitializer, testInPropertyGetterWithExpressionBody, testInTopLevelPropertyInitializer, testNnStringVsTXArray, testNnStringVsTXString |
| OTHER | 8 | testFunctionAssertion, testJavaNestedSamInterface, testKt43217, testKt53041, testOverrideWithArrayParameterType2, testRawTypeArgumentInJavaSuperType, testTriangleWithFlexibleTypeAndSubstitution4, testUsingNullableValueAsLowerBoundLeadsToNullableResult2 |
| NONE_APPLICABLE | 4 | testAddedOverloadWithAtomics, testIntersectionKotlinJavaAtomics, testKotlinToJavaHierarchy, testKt48590 |
| UNRESOLVED_REFERENCE | 3 | testGenericSamProjectedOut, testJavaInterfaceFieldDirectAccess, testKt65482 |
| ARGUMENT_TYPE_MISMATCH | 2 | testFunctionWithBigArity, testLambdaInstanceOf |
| CANNOT_INFER_PARAMETER_TYPE | 2 | testSamTypeParameter, testUsingNullableValueAsLowerBoundLeadsToNullableResult |
| NOTHING_TO_OVERRIDE | 1 | testPrimitiveSubstitutionToDnnParameter |
| AbstractMethodError | 1 | testDelegationToJavaDnn |

### Analysis of Remaining Issues

1. **MISSING_DEPENDENCY_CLASS (13)**: Likely type parameters (`T`, `U`) being treated as class names rather than type variables.

2. **NPE_ASSERTION (8)**: Enhanced nullability assertion tests - NPE not being thrown when expected. Related to annotation processing.

3. **NONE_APPLICABLE (4)**: Overload resolution issues, likely related to atomics handling.

4. **UNRESOLVED_REFERENCE (3)**: Interface fields or generic type access issues.

5. **ARGUMENT_TYPE_MISMATCH (2)**: Generic type argument handling (FunctionN issues).

### Key Learnings
1. **Reserved words in module paths**: Java 9 introduced module system with reserved words like `kotlin`, `java`, `javax`. Parsers may treat these specially.
2. **ERROR_ELEMENT recovery**: Parser errors don't mean data is lost - the AST often contains recoverable information in ERROR_ELEMENT nodes.
3. **Exception-based debugging is essential**: Adding `throw IllegalStateException("DEBUG: ${node.dump()}")` was the only reliable way to see AST structure in Gradle test output.

---

## Iteration 7a: Array Types and Vararg Handling - 2026-03-04

### Status
- ✅ Completed

### Summary
Implemented array type parsing and vararg handling in `createJavaType()`. Fixed method/constructor/class type parameters to include localScope and imports for proper bound resolution. Improved from 90/138 (65.2%) to 96/138 (69.6%) - gained 6 tests.

### Key Findings
1. **Array AST Structure**: Arrays are represented as `TYPE` containing nested `TYPE` + `LBRACKET`/`RBRACKET`:
   ```
   TYPE: String[]
     TYPE: String
       JAVA_CODE_REFERENCE: String
     LBRACKET: [
     RBRACKET: ]
   ```

2. **Vararg AST Structure**: Varargs use `ELLIPSIS` instead of brackets, inside the TYPE node:
   ```
   TYPE: String...
     TYPE: String
       JAVA_CODE_REFERENCE: String
     ELLIPSIS: ...
   ```

3. **TYPE Node Handling Bug**: When `createJavaType()` receives a TYPE node directly (e.g., from parameter), calling `findChildByType("TYPE")` returns the nested component type, skipping the array dimension. Fix: check if input node IS a TYPE with LBRACKET/ELLIPSIS before looking for nested TYPE.

4. **Type Parameter Scope**: `JavaTypeParameterOverAst` needs `localScope` and `imports` to resolve bounds like `<T extends SomeClass>`.

### Changes Made
| File | Change |
|------|--------|
| `JavaTypeOverAst.kt` | Added array type detection (LBRACKET) and vararg detection (ELLIPSIS) in `createJavaType()`. Updated `JavaTypeParameterOverAst` constructor to accept localScope/imports. |
| `JavaMemberOverAst.kt` | Fixed `isVararg` to check ELLIPSIS inside TYPE node. Updated method/constructor typeParameters to pass localScope/imports. |
| `JavaClassOverAst.kt` | Updated class-level typeParameters to pass localScope/imports to `JavaTypeParameterOverAst`. |

### Test Results
- Box tests: 96/138 passing (69.6%) - UP from 90/138 (65.2%)
- Gained: 6 tests (+4.4%)

### Tests Fixed
- `testOverrideWithArrayParameterType` - String[] parameter now correctly parsed as Array<String>
- `testOverrideWithArrayParameterTypeNotNull` - Array with nullability annotations
- `testOverrideWithVarargParameterType` - String... vararg now correctly parsed as Array<String>
- Plus 3 other tests benefiting from array/vararg handling

### Tests Still Failing (42 remaining)
| Category | Count | Notes |
|----------|-------|-------|
| MISSING_DEPENDENCY_CLASS | ~15 | Kotlin classes from Java (kotlin.Function, etc.) |
| CANNOT_INFER_PARAMETER_TYPE | ~5 | Complex generic inference |
| NOTHING_TO_OVERRIDE | ~3 | Raw types (List...) not handled |
| Other | ~19 | Various issues |

### Issues Encountered
1. **isVararg returning false**: ELLIPSIS was inside TYPE node, not direct child of PARAMETER. Fixed by checking both locations.
2. **testOverrideWithArrayParameterType2 still fails**: Uses raw type `List...` which needs proper raw type handling (out of scope for this iteration).

### Key Learnings
1. Always check if input node is already the target type before calling `findChildByType()` - it may skip important structure.
2. Vararg and array have different AST representations but both need to produce `JavaArrayType`.
3. Exception-based debugging with `node.dump()` is essential for understanding AST structure.
