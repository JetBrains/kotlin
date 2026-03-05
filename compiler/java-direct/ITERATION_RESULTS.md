# Java-Direct: Iteration Results Log

## Document Purpose

This file captures key findings, decisions, and learnings from each iteration. It serves as:
1. **Progress tracker**: What's been completed
2. **Knowledge base**: Discoveries about the codebase and architecture
3. **Context updater**: Input for updating AGENT_INSTRUCTIONS.md after multiple iterations

**Usage**: After completing each iteration, the agent MUST append a results section below.

**Last Updated**: 2026-03-05 (Iteration 13 complete)

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
| 7a-c | 2026-03-04 | Arrays, Imports, Type Params | 90/138 | 101/138 | Array/vararg, ERROR_ELEMENT imports, type param scope |
| 8 | 2026-03-04 | Annotations & Nullability | 101/138 | 111/138 | TYPE_USE annotations, fragmented imports |
| 9 | 2026-03-04 | Interface Fields/Methods | 111/138 | 115/138 | Implicit static/final/abstract modifiers |
| 10 | 2026-03-04 | Nested Interfaces/Enums | 115/138 | 117/138 | Implicit static for nested interfaces/enums |
| 11 | 2026-03-05 | External Type Arguments | 117/138 | 128/138 | Fixed type args in null classifier branch |
| 12 | 2026-03-05 | Fragmented Star Imports | 128/138 | 128/138 | Fixed star import parsing (annotation fix superseded by #13) |
| **13** | **2026-03-05** | **Annotation Callback Resolution** | **511/601** | **511/601** | **Unified annotation resolution with type resolution** |

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

## Iteration 13: Unified Annotation Resolution via Callback - 2026-03-05

### Status
- ✅ Completed

### Summary
Refactored annotation resolution to use the same callback-based pattern as type resolution. Previously, iteration 12 added hardcoded java.lang annotation mappings and known annotation package lists. Per user feedback, this was incorrect - annotations should resolve the same way as regular type declarations via `resolveWithCallback`. Added `isResolved` and `resolveAnnotation(tryResolve)` to `JavaAnnotation` interface, mirroring the pattern used by `JavaClassifierType`. FIR now calls `resolveAnnotation` with a callback that checks symbol existence.

### Key Findings

1. **Annotation Resolution Should Match Type Resolution**: The user correctly pointed out that hardcoded annotation packages (lombok, javax.annotation, etc.) was wrong. Annotations should use the same resolution mechanism as types - check same package, java.lang, and star imports via callback.

2. **JavaClassifierType Pattern**: `JavaClassifierType` already has `isResolved` property and `resolve(tryResolve: (String) -> Boolean)` method. Applied same pattern to `JavaAnnotation`.

3. **FIR Integration Point**: `javaAnnotationsMapping.kt`'s `buildFirAnnotation()` function is where annotation ClassId is determined. Added resolution callback that uses `session.symbolProvider.getClassLikeSymbolByClassId()` to check existence.

4. **Remaining Failures are Raw Types**: After this fix, 6 box tests and 84 diagnostic tests fail. Most are related to raw type handling (using generic types without type arguments).

### Changes Made

| File | Change |
|------|--------|
| `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaElements.kt` | Added `isResolved` property and `resolveAnnotation(tryResolve)` method to `JavaAnnotation` interface with default implementations |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaAnnotationOverAst.kt` | Implemented `isResolved` (checks if FQN or has simple import) and `resolveAnnotation` (uses `resolutionContext.resolveWithCallback`) |
| `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/javaAnnotationsMapping.kt` | Updated `buildFirAnnotation` to call `resolveAnnotation` when `isResolved` is false, using symbolProvider callback |

### Test Results
- Box tests: **136/142 passing (95.8%)**
- Diagnostic tests: **344/428 passing (80.4%)**
- Total: **511/601 passing (85.0%)**

### Remaining Box Test Failures (6 tests)

| Test | Issue |
|------|-------|
| testConstValAsAnnotationArgumentInJava | Annotation argument handling |
| testInheritanceWithWildcard | NoSuchMethodError - IR fake override |
| testKjkWithRawTypes | Raw generic types |
| testKt48590 | NONE_APPLICABLE - overload resolution |
| testOverrideWithArrayParameterType2 | Raw type with array |
| testRawTypeArgumentInJavaSuperType | Raw type in supertypes |

Most remaining failures are related to **raw types** - Java generics used without type arguments.

### Key Learnings

1. **Consistency Over Special Cases**: The user's feedback was correct - adding hardcoded package lists creates maintenance burden and inconsistency. Using the same resolution mechanism for all symbols is cleaner.

2. **Interface Extension Pattern**: Adding optional methods to interfaces with default implementations allows gradual adoption without breaking existing implementations.

3. **Callback Pattern is Powerful**: The `tryResolve: (String) -> Boolean` callback pattern allows the Java Model to implement Java-specific resolution rules while FIR validates symbol existence without tight coupling.

---

## Iteration 12: Fragmented Star Import Parsing - 2026-03-05

### Status
- ✅ Completed (annotation handling superseded by Iteration 13)

### Summary
Fixed fragmented star import parsing where the KMP parser splits `import org.jetbrains.annotations.*;` across multiple sibling nodes (ERROR_ELEMENT, TYPE, ERROR_ELEMENT). Also fixed handling of empty ERROR_ELEMENT nodes during import scanning.

**Note**: This iteration initially also added hardcoded java.lang annotation mappings and known annotation package lists, but that approach was **superseded by Iteration 13** which unified annotation resolution with the existing type resolution callback pattern.

### Key Findings

1. **Fragmented Star Import Pattern**: When parsing `import org.jetbrains.annotations.*;`, the KMP parser sometimes produces:
   ```
   ERROR_ELEMENT:import
   WHITE_SPACE: 
   MODIFIER_LIST:
   TYPE:org.jetbrains.annotations.
   ERROR_ELEMENT:
   ERROR_ELEMENT:*;
   ```
   The star import package was not being captured because the code expected an IMPORT_STATEMENT node.

2. **Empty ERROR_ELEMENT Nodes**: Between the TYPE and the ERROR_ELEMENT containing `*;`, there's often an empty ERROR_ELEMENT that was causing the scan to stop prematurely.

3. **Enhanced Nullability Impact**: Without proper star import resolution, `@NotNull` from `org.jetbrains.annotations` couldn't be resolved, preventing FIR from applying enhanced nullability.

### Changes Made (Retained)

| File | Change |
|------|--------|
| `JavaResolutionContext.kt` | Fixed fragmented star import extraction to skip empty ERROR_ELEMENT nodes and properly detect `*;` in subsequent ERROR_ELEMENT siblings. |
| `JavaParsingTest.kt` | Updated test expectation for `@Deprecated` to expect `java.lang.Deprecated`. |

### Changes Made (Superseded by Iteration 13)

The following changes were later replaced with a proper callback-based solution:
- Hardcoded java.lang annotation mappings in `JavaAnnotationOverAst.classId`
- Known annotation package lists for star import resolution

### Key Learnings

1. **Parser Fragmentation is Common**: The KMP Java parser fragments imports and other constructs more often than expected, especially for imports starting with reserved-like words or containing stars.

2. **Avoid Hardcoded Special Cases**: Hardcoding known packages (lombok, javax.annotation, etc.) creates maintenance burden. Better to use the same resolution mechanism for all symbols (fixed in Iteration 13).

3. **Debug via Exception**: Adding `throw IllegalStateException("DEBUG: ...")` remains the most reliable way to inspect intermediate values in Gradle test output.

---

## Iteration 11: External Type Arguments Fix - 2026-03-05

### Status
- ✅ Completed

### Summary
Fixed a critical bug where type arguments for external Java types (JDK classes like `java.util.AbstractMap<K,V>`) were being dropped during FIR type conversion. When `JavaClassifierType.classifier` returned `null` (indicating an external type not available in source), the code was ignoring `typeArguments` and constructing the type with `emptyArray()`. This caused type parameter substitution failures like `K` instead of `Double`. **Massive improvement**: Box tests 117→128 (+11), Diagnostic tests 264→331 (+67).

### Key Findings

1. **Root Cause Discovery**: The test `testMapGetOverride` was failing with:
   ```
   ARGUMENT_TYPE_MISMATCH: Argument type mismatch: actual type is 'Double', 
   but 'K! (of class AbstractMap<K : Any!, V : Any!>)' was expected.
   ```
   This showed type parameters (`K`) weren't being substituted with concrete types (`Double`).

2. **AST Parsing Was Correct**: Debug logging confirmed java-direct was correctly parsing the supertype:
   ```kotlin
   // class MyMap extends java.util.AbstractMap<Double, CharSequence>
   typeArgs=[JavaClassifierTypeOverAst:Double, JavaClassifierTypeOverAst:CharSequence]
   ```

3. **Bug Location**: `JavaTypeConversion.kt` (FIR layer, not java-direct). In the `null` classifier branch (lines 249-268), when converting a `JavaClassifierType` where `classifier` is `null` (external types), the code was:
   ```kotlin
   null -> {
       val qualifiedName = this.classifierQualifiedName
       // ... resolve classId ...
       classId.constructClassLikeType(emptyArray(), ...)  // BUG: ignoring typeArguments!
   }
   ```

4. **The `is JavaClass` Branch Had It Right**: The branch handling `classifier is JavaClass` (around line 190) correctly processed type arguments with raw type handling, wildcard conversion, etc. The `null` branch needed the same logic.

5. **Why `null` Classifier?**: java-direct returns `classifier = null` for types whose classes aren't available in parsed source files (e.g., JDK classes). It provides `classifierQualifiedName` (e.g., `java.util.AbstractMap`) so FIR can resolve the type, but was ignoring the type arguments.

### Changes Made

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` | In the `null` classifier branch, added full type argument handling: Java→Kotlin mapping, raw type detection, wildcard conversion, and proper `mappedTypeArguments` construction. |

### Code Change

**Before** (lines 249-268):
```kotlin
null -> {
    val qualifiedName = this.classifierQualifiedName
    var classId = if (!isResolved && ...) { ... } else { ... }
    classId = JavaToKotlinClassMap.mapJavaToKotlin(...) ?: classId
    // ... mutable handling ...
    classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
}
```

**After**:
```kotlin
null -> {
    val qualifiedName = this.classifierQualifiedName
    var classId = if (!isResolved && ...) { ... } else { ... }
    classId = if (mode.insideAnnotation) {
        JavaToKotlinClassMap.mapJavaToKotlinIncludingClassMapping(classId.asSingleFqName())
    } else {
        JavaToKotlinClassMap.mapJavaToKotlin(classId.asSingleFqName())
    } ?: classId
    
    if (lowerBound == null || argumentsMakeSenseOnlyForMutableContainer(classId, session)) {
        classId = classId.readOnlyToMutable() ?: classId
    }
    
    val lookupTag = classId.toLookupTag()
    val mappedTypeArguments = when {
        isRaw -> {
            // Raw type handling - same as JavaClass branch
            val typeParameterSymbols = lookupTag.takeIf { ... }?.toRegularClassSymbol(session)?.typeParameterSymbols
            when {
                mode.insideAnnotation -> typeParameterSymbols?.let { Array(it.size) { ConeStarProjection } }
                else -> typeParameterSymbols?.getProjectionsForRawType(session, nullabilities = null)
            }
        }
        lookupTag != lowerBound?.lookupTag && typeArguments.isNotEmpty() -> {
            // Type argument conversion - same as JavaClass branch
            val typeParameterSymbols = lookupTag.takeIf { ... }?.toRegularClassSymbol(session)?.typeParameterSymbols
            Array(typeArguments.size) { index ->
                val newMode = if (mode.insideAnnotation) FirJavaTypeConversionMode.DEFAULT else mode
                val argument = typeArguments[index]
                val variance = typeParameterSymbols?.getOrNull(index)?.fir?.variance ?: Variance.INVARIANT
                argument.toConeTypeProjection(session, javaTypeParameterStack, variance, newMode, source)
            }
        }
        else -> lowerBound?.typeArguments
    }
    
    lookupTag.constructClassType(mappedTypeArguments ?: ConeTypeProjection.EMPTY_ARRAY, isMarkedNullable = lowerBound != null, attributes)
}
```

### Test Results
- Box tests: **128/138 passing (92.8%)** - UP from 117/138 (84.8%) - **+11 tests**
- Diagnostic tests: **331/428 passing (77.3%)** - UP from 264/428 (61.7%) - **+67 tests**
- **Total: 459/566 (81.1%) → 459/566 tests analyzed, 78 tests fixed**

### Tests Fixed (Examples)
| Test | Issue Fixed |
|------|-------------|
| `testMapGetOverride` | `AbstractMap<Double, CharSequence>` type args now work |
| `testListIteratorWithPlatformTypes` | `ArrayList<E>` type parameter substitution |
| `testSubclassOfMapEntry` | `Map.Entry<K, V>` nested generic types |
| Multiple collection override tests | Generic JDK collection inheritance |

### Remaining Failures (10 box tests)

| Category | Count | Tests | Root Cause |
|----------|-------|-------|------------|
| **Atomics Mapping** | 5 | testKotlinToJavaHierarchy, testIntersectionKotlinJavaAtomics, testJavaToKotlinHierarchy, testUsingJavaAtomicWhenKotlinAtomicExpected, testUsingKotlinAtomicWhenJavaAtomicExpected | FIR's Kotlin↔Java atomic type mapping (not java-direct issue) |
| **Raw Types** | 3 | testKjkWithRawTypes, testOverrideWithGenericArrayParameterType, testRawTypeArgumentInJavaSuperType | FIR raw type erasure handling |
| **Wildcards** | 1 | testInheritanceWithWildcard | NoSuchMethodError - IR fake override issue |
| **Other** | 1 | testKt48590 | NONE_APPLICABLE - overload resolution |

### Key Learnings

1. **Debug the Right Layer**: The bug wasn't in java-direct parsing (which was correct) but in FIR's type conversion. Exception-based debugging at the boundary helped identify this.

2. **Follow the Data Flow**: `JavaClassifierType.typeArguments` → `JavaTypeConversion.toFirResolvedTypeRef()` → `ConeClassLikeType.typeArguments`. The `null` branch wasn't completing this flow.

3. **Code Duplication Smell**: The `is JavaClass` and `null` branches had almost identical logic, but `null` was missing critical parts. This was a classic "incomplete copy" bug.

4. **External vs Source Types**: java-direct returns `classifier=null` for external types (JDK, libraries) but still provides `classifierQualifiedName` and `typeArguments`. FIR must handle both paths identically.

### Architecture Insight

The `JavaClassifierType.classifier` property has three possible states:
1. `is JavaClass` - Source class available, full resolution possible
2. `is JavaTypeParameter` - Type parameter reference
3. `null` - External type (JDK, library), only `classifierQualifiedName` available

All three branches must handle `typeArguments` consistently. The bug was that only the first branch did.

---

## Iteration 10: Nested Interfaces and Enums Implicitly Static - 2026-03-04

### Status
- ✅ Completed

### Summary
Fixed `isStatic` for nested interfaces and enums to return `true` even without explicit `static` keyword. In Java, nested interfaces and enums are implicitly static. This affects FIR's `isInner` determination (`isInner = !isTopLevel && !isStatic`), which in turn affects IR's `extractTypeParameters()` during fake override building for SAM conversion. Improved from 115/138 (83.3%) to 117/138 (84.8%) - gained 2 tests.

### Key Findings

1. **Root Cause Analysis**: `testJavaNestedSamInterface` was failing with:
   ```
   IllegalArgumentException: typeParameters = [2 params] size != typeArguments = [] size
   at FakeOverrideBuilderStrategy.kt:183
   ```
   This happens in IR's `buildFakeOverrideMember()` when building the SAM fake override.

2. **The `extractTypeParameters()` Function**: At `IrTypeSystemContext.kt:596-619`, this function collects type parameters from the class and its enclosing classes. For non-inner classes (`!current.isInner`), it stops at that class. For inner classes, it continues to the outer class.

3. **FIR's `isInner` Determination**: In `FirJavaFacade.kt:188`:
   ```kotlin
   this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
   ```
   So `isInner = true` when the class is NOT top-level AND NOT static. For a nested interface, if `isStatic` returns `false`, FIR sets `isInner = true`.

4. **Java Language Rule**: Nested interfaces and enums are **implicitly static** in Java, even without the `static` keyword. This is different from nested classes which require explicit `static` to be static.

5. **Impact Chain**:
   - java-direct `isStatic = false` for nested interface (bug)
   - FIR sets `isInner = true`
   - IR's `extractTypeParameters()` collects outer class type params too
   - SAM fake override builder expects type args matching all type params
   - No type args provided → crash

### Changes Made

| File | Change |
|------|--------|
| `JavaClassOverAst.kt` | Updated `isStatic` property to also return `true` for nested interfaces and enums: `hasModifier("STATIC_KEYWORD") || (outerClass != null && (isInterface || isEnum))` |
| `JavaClassOverAst.kt` | Updated `findInnerClass()` to treat interfaces and enums as effectively static (for resolution context selection) - already done in previous iteration but now consistent with `isStatic` |

### Test Results
- Box tests: 117/138 passing (84.8%) - UP from 115/138 (83.3%)
- Gained: 2 tests (+1.5%)

### Tests Fixed
| Test | Issue Fixed |
|------|-------------|
| `testJavaNestedSamInterface` | Nested SAM interface now correctly `isStatic=true`, so FIR sets `isInner=false`, IR doesn't collect outer type params |
| `testKt53041` | Related nested class/interface type parameter handling |

### Remaining Failures (21 tests)

| Category | Count | Example Tests |
|----------|-------|---------------|
| NotNullAssertions | 4 | testLocalEntities, testFunctionAssertion, testFunctionWithBigArity, testNoAssertionForNulllableCaptured |
| CommonAtomicTypes | 2 | testUsingJavaAtomicWhenKotlinAtomicExpected, testUsingKotlinAtomicWhenJavaAtomicExpected |
| OTHER | 15 | Various inheritance, raw types, and generic issues |

### Architecture Insight

The `isStatic` flag has far-reaching effects:

```
JavaClass.isStatic
    ↓
FirJavaFacade: isInner = !isTopLevel && !isStatic
    ↓
Fir2IrClassifiersGenerator: irClass.isInner = regularClass.isInner
    ↓
IR extractTypeParameters(): if (!current.isInner) stop else continue to outer
    ↓
buildFakeOverrideMember(): requires typeParams.size == typeArgs.size
```

For nested interfaces/enums, the correct chain is:
- `isStatic = true` (implicit in Java)
- `isInner = false`
- `extractTypeParameters()` stops at the nested interface
- SAM fake override builds correctly

### Key Learnings

1. **Java Implicit Static Rules**:
   - Nested interfaces: always implicitly static
   - Nested enums: always implicitly static  
   - Nested classes: only static if explicitly marked

2. **isStatic Affects isInner**: The `isStatic` flag in Java Model directly controls the FIR `isInner` status, which has cascading effects on IR type parameter collection.

3. **Consistency Matters**: Both `isStatic` property and `findInnerClass()` resolution context selection should agree on whether a nested type is static. The previous iteration fixed `findInnerClass()` but not `isStatic`, causing inconsistency.

4. **Debug via Exception**: The error message `typeParameters = [2 params] size != typeArguments = [] size` was the key clue - 2 type params meant both outer class `X` and inner interface `T` were being collected, indicating the interface was incorrectly treated as inner.

---

## Iteration 9: Interface Fields and SAM Conversion - 2026-03-04

### Status
- ✅ Completed

### Summary
Fixed interface field and method implicit modifiers for proper static field access and SAM conversion. Interface fields are implicitly `public static final` and interface methods without a body are implicitly abstract. These fixes enable FIR to correctly identify SAM (Single Abstract Method) interfaces. Improved from 111/138 (80.4%) to 115/138 (83.3%) - gained 4 tests.

### Key Findings

1. **Interface Fields Implicitly Static/Final**: In Java, interface fields like `String CONST = "value";` are implicitly `public static final`. Without this, FIR couldn't find interface fields via `PublicParentInterface.publicStaticField` causing `UNRESOLVED_REFERENCE`.

2. **Interface Methods Implicitly Abstract**: Interface methods without a body are implicitly abstract. FIR's SAM resolver checks `isFun` (set to `true` for all Java interfaces) and then looks for a single abstract method via `getSingleAbstractMethodOrNull()`. Without `isAbstract=true`, methods weren't counted as abstract and SAM conversion failed with `UNRESOLVED_REFERENCE: Unresolved reference 'it'`.

3. **SAM Resolution Flow**: 
   - `FirJavaFacade` sets `isFun = classKind == ClassKind.INTERFACE` for all Java interfaces
   - `FirSamResolver.resolveFunctionTypeIfSamInterface()` checks `firRegularClass.status.isFun` 
   - Then calls `getSingleAbstractMethodOrNull()` which needs exactly one abstract method
   - If found, SAM conversion works and lambda `it` parameter gets its type inferred

4. **hasBody Check**: Interface methods can have default implementations (Java 8+) with a `CODE_BLOCK`. Only methods without `CODE_BLOCK` should be implicitly abstract.

### Changes Made

| File | Change |
|------|--------|
| `JavaMemberOverAst.kt` | `JavaFieldOverAst`: Added `isStatic` and `isFinal` overrides that return `true` for interface fields. `JavaMethodOverAst`: Added `isAbstract` override that returns `true` for interface methods without body, added `hasBody` property checking for `CODE_BLOCK`. |

### Test Results
- Box tests: 115/138 passing (83.3%) - UP from 111/138 (80.4%)
- Gained: 4 tests (+2.9%)

### Tests Fixed
| Test | Issue Fixed |
|------|-------------|
| `testJavaInterfaceFieldDirectAccess` | Interface field now correctly marked as static |
| `testKt65482` | Interface field access via qualified name |
| 2 SAM-related tests | Interface methods now correctly marked as abstract |

### Remaining Failures (23 tests)

| Category | Count | Example Tests |
|----------|-------|---------------|
| OTHER | 9 | testKt53041, testJavaNestedSamInterface, testRawTypeArgumentInJavaSuperType |
| CANNOT_INFER_PARAMETER_TYPE | 4 | testSamTypeParameter, testLocalEntities, testFunctionAssertion |
| NONE_APPLICABLE | 3 | testKt48590, testKotlinToJavaHierarchy, testIntersectionKotlinJavaAtomics |
| MISSING_DEPENDENCY_CLASS | 3 | testJavaToKotlinHierarchy, atomics tests |
| ARGUMENT_TYPE_MISMATCH | 2 | testLambdaInstanceOf, testFunctionWithBigArity |
| ASSIGNMENT_TYPE_MISMATCH | 1 | testGenericSamProjectedOut |
| NoSuchMethodError | 1 | testInheritanceWithWildcard |

### Analysis of Remaining Issues

1. **Generic projection issues** (testGenericSamProjectedOut): SAM conversion works but type parameter substitution with projections (`out String`) doesn't match expected type.

2. **Nested SAM interfaces** (testJavaNestedSamInterface): `A<!>.I<String!>` - nested interface type has platform type issue with outer class type parameter.

3. **CANNOT_INFER_PARAMETER_TYPE**: Complex generic inference scenarios with inheritance chains.

4. **MISSING_DEPENDENCY_CLASS for atomics**: Kotlin/Java atomic types mapping not handled.

5. **Raw types** (testKjkWithRawTypes, testRawTypeArgumentInJavaSuperType): Raw generic types not properly handled.

### Key Learnings

1. **Java Language Implicit Modifiers**: Interface members have implicit modifiers that must be explicitly handled:
   - Fields: implicitly `public static final`
   - Methods without body: implicitly `public abstract`
   - Methods with body (default methods): implicitly `public` but NOT abstract

2. **SAM Detection Chain**: The SAM detection requires:
   - Interface (check)
   - `isFun = true` (automatically set for Java interfaces)
   - Exactly one abstract method (requires correct `isAbstract` reporting)

3. **Debug via Exception**: Exception-based debugging (`throw IllegalStateException("DEBUG: ...")`) remains the only reliable way to inspect values in Gradle test output.

---

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
