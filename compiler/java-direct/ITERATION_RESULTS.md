# Java-Direct: Iteration Results Log

## Document Purpose

Append-only log of iteration findings, decisions, and learnings.

**Current status**: See `FIXING_ITERATIONS.md` for up-to-date test counts and remaining work.

**Last Updated**: 2026-03-16

---

## Iteration 36: Visibility and Enum Entries — 2026-03-16

### Root Cause Analysis

Three independent visibility/enum bugs:

**Bug 1 — Java enum `entries` property (NoSuchFieldError)**
`FirJavaFacade.kt` used `Java.Source` origin for the synthetic `entries` getter of java-direct source enums.
`Java.Source` is a `FirDeclarationOrigin.Java` subtype → `isJavaOrigin=true` in `createPropertySymbols` → no getter symbol created, backing field `entries` created instead → JVM codegen generates `GETSTATIC MyEnum.entries` → `NoSuchFieldError` at runtime.
Using `Library` origin (non-Java) avoids the backing field and creates a getter, which `EnumExternalEntriesLowering` then correctly intrinsifies.
Note: `Source` origin is blocked because `FirPropertyAccessorImpl` validates `source != null` for Source origin.

**Bug 2 — Enum constant visibility (INVISIBLE_REFERENCE)**
`JavaFieldOverAst` had no visibility override for enum entries. They have no explicit modifiers in the AST → fell through to `PackageVisibility`. Java spec (JLS 8.9.3): enum constants are implicitly `public static final`.

**Bug 3 — Nested class visibility (INVISIBLE_REFERENCE)**
`JavaClassOverAst.visibility` had two issues:
- Nested types in interfaces should be `Public` (JLS 9.5): missing `outerClass?.isInterface == true` → Public case
- Protected nested classes returned `Visibilities.Protected` (subclasses only), should be `ProtectedAndPackage` or `ProtectedStaticVisibility` to also allow same-package access

### Fix

**FirJavaFacade.kt**: Changed `enumEntriesOrigin` for java-direct source classes from `Java.Source` to `Library`:
```kotlin
val enumEntriesOrigin = when {
    firJavaClass.origin.fromSource && classSource != null -> FirDeclarationOrigin.Source
    else -> FirDeclarationOrigin.Library
}
```

**JavaMemberOverAst.kt** (`JavaFieldOverAst`): Added visibility override for enum entries:
```kotlin
override val visibility: Visibility get() = if (isEnumEntry) Visibilities.Public else super.visibility
```

**JavaClassOverAst.kt**: Fixed nested class visibility:
```kotlin
override val visibility: Visibility
    get() = when {
        outerClass?.isInterface == true -> Visibilities.Public
        hasModifier("PUBLIC_KEYWORD") -> Visibilities.Public
        hasModifier("PROTECTED_KEYWORD") -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
        hasModifier("PRIVATE_KEYWORD") -> Visibilities.Private
        else -> JavaVisibilities.PackageVisibility
    }
```

### Test Results
- **Box tests**: 13 → 11 (+2 fixed: `testEnumEntriesFromJava`, `testStaticImportFromEnumJava`, `testJavaAnnotationConstructorTypes`, `testJavaVisibility`)
- **Phased tests**: 80 → 68 (+12 fixed: enum visibility, nested interface type visibility, protected member visibility across multiple test suites)
- **Total failures**: 93 → 79 (14 tests fixed)
- PSI regression tests: All passing ✅

### Files Modified
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt` — enumEntriesOrigin fix
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — enum constant visibility
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — nested class visibility

### Key Learnings
1. **FirDeclarationOrigin.Java subtypes suppress getter creation** — `isJavaOrigin` check blocks getter symbol for Java-origin properties, creating backing fields instead; synthetic properties like `entries` must use non-Java origin
2. **Implicit member modifiers in Java** — enum constants (public), interface members (public), interface nested types (public) have no explicit modifiers in AST; all need explicit rules
3. **Protected in Java means package+protected** — `Visibilities.Protected` only covers subclasses; Java's `protected` is `ProtectedAndPackage`

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

## Iteration 31: Fixed JavaParsingTest Regressions

**Goal**: Fix 9 JavaParsingTest failures that appeared after implementing cross-file ambiguity detection

**Root cause analysis**: The JAVA_LANG_TYPES eager resolution (added for `hasConstantNotNullInitializer` support) created an inconsistency:
- `classifierQualifiedName` returned "java.lang.Object" (qualified) for unqualified "Object"
- `isResolved` returned `false` (unresolved)
- Tests expected unresolved types to keep their unqualified names until explicit resolution via callback

**The problem**: JAVA_LANG_TYPES map was added to help `hasConstantNotNullInitializer` identify String types early (before FIR resolution), but it violated the lazy resolution contract that `classifierQualifiedName` should return raw type names for unresolved types.

**Fixes applied**:
1. **Removed JAVA_LANG_TYPES from `classifierQualifiedName`** (JavaTypeOverAst.kt):
   - Removed step 4 that eagerly resolved java.lang types
   - Now returns raw type name as written in source
   - Resolution happens lazily via `resolve()` callback (which already handles java.lang)

2. **Updated `hasConstantNotNullInitializer`** (JavaMemberOverAst.kt):
   - Now accepts both "String" (unresolved) and "java.lang.String" (resolved)
   - Works correctly without eager resolution

3. **Fixed `testLocalInheritance`** (JavaParsingTest.kt):
   - Updated to expect implicit java.lang.Object supertype for Base class
   - This implicit supertype was added in iteration 21 (correct Java semantics)

**Test results**: 
- Before: 40 tests, 9 failures
- After: **40 tests, all passing** ✅

**Failing tests that were fixed**:
- `testTypeResolution` - Expected unqualified "Object", not "java.lang.Object"
- `testTypeNameStripsTypeArguments` - Type name qualification issue
- `testAnnotatedTypeArguments` - Type name qualification issue
- `testAnnotatedTypeArgumentsMultiple` - Type name qualification issue
- `testLocalInheritance` - Expected implicit Object supertype
- `testSimpleTypeArguments` - Type name qualification issue
- `testMethodParametersWithObjectType` - Type name qualification issue
- `testUnboundedWildcard` - Type name qualification issue
- `testMethodParameters` - Type name qualification issue

**Files modified**:
- `JavaTypeOverAst.kt` - removed eager java.lang resolution from `classifierQualifiedName`
- `JavaMemberOverAst.kt` - updated String check to handle both qualified and unqualified
- `JavaParsingTest.kt` - updated `testLocalInheritance` expectations

### Key Learnings
1. **Maintain consistency** between `classifierQualifiedName` and `isResolved` - they must agree on resolution state
2. **Lazy resolution is the contract** - unresolved types should keep their raw names until explicitly resolved
3. **Targeted fixes over broad heuristics** - updated `hasConstantNotNullInitializer` to handle both cases instead of forcing eager resolution
4. **Regression testing is critical** - JavaParsingTest caught the inconsistency that cross-file changes introduced

---

*For detailed iteration histories, see `implDocs/archive/`*

## Iteration 32 (Final): Kotlin Constants in Java Annotations

**Goal**: Support Kotlin `const val` in Java annotations without causing regressions

**Problem**: Java annotations using Kotlin constants (e.g., `@Foo(KotlinClass.FOO_INT)`) failed

**Solution** (targeted, no new Java Model interfaces):
1. **Reused `JavaEnumValueAnnotationArgument`** for both enums AND const fields
2. **Extended FIR mapping**: Check if symbol is const property before creating enum entry
   - Looks in both class declarations and companion object
   - Extracts `FirLiteralExpression.value` and returns as constant
3. **Targeted fix in annotation resolution**: Added special handling in `JavaEnumValueAnnotationArgumentOverAst.resolveEnumClass()`
   - When in default package and standard resolution fails, try simple name directly
   - Avoids broad changes to general name resolution that caused 99 test regressions

**Results**: ✅ **Net improvement: 108 → 106 failures (-2)**
- Box tests: 16 → 16 failures (no change)
- Phased tests: 92 → 90 failures (-2)
- Fixed annotation tests:
  - ✅ `testAnnotationWithKotlinProperty`
  - ✅ `testAnnotationWithKotlinPropertyFromInterfaceCompanion`
  - ❌ `testConstValAsAnnotationArgumentInJava` (requires static import support)
  - ❌ `testAnnotationsViaActualTypeAliasFromBinary` (binary/typealias issue)

**Files modified**:
- `compiler/java-direct/src/.../JavaAnnotationOverAst.kt` - targeted root package fix in resolveEnumClass
- `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt` - const field detection and value extraction

**Known limitation**: Static imports still unsupported (need static import tracking infrastructure).

**Key lesson**: Targeted fixes in specific code paths are better than broad changes to core resolution logic.

---

## Iteration 33: Raw Types Detection Fix - 2026-03-16

### Status
✅ Complete — 10 tests fixed

### Root Cause Analysis

Two separate bugs were causing raw type detection failures:

**Bug 1: Java Model `isRaw` detection (JavaTypeOverAst.kt)**

The `REFERENCE_PARAMETER_LIST` node exists even when empty (no type arguments). The old check:
```kotlin
val hasParameterList = node.findChildByType("REFERENCE_PARAMETER_LIST") != null
!hasParameterList && (classifier as? JavaClass)?.typeParameters?.isNotEmpty() == true
```
Always returned `false` for raw types because `hasParameterList` was `true` even for empty parameter lists.

Debug output showed:
```
JAVA_CODE_REFERENCE: 'Generic'
  IDENTIFIER: 'Generic'
  REFERENCE_PARAMETER_LIST: ''   <-- Empty but still present!
```

**Bug 2: FIR raw type detection for unresolved types (JavaTypeConversion.kt)**

For types resolved via star imports (e.g., `List` from `import java.util.*;`), `classifierQualifiedName` returns the simple name `"List"`, not `"java.util.List"`. The raw type detection at line 158:
```kotlin
val classId = ClassId.topLevel(FqName(classifierQualifiedName))
```
Created `ClassId(FqName(""), Name("List"))` — a class named "List" in the default package, which doesn't exist.

### Fix

**1. JavaTypeOverAst.kt** — Check for TYPE children, not just parameter list existence:
```kotlin
override val isRaw: Boolean by lazy {
    val parameterList = node.findChildByType("REFERENCE_PARAMETER_LIST")
    val hasTypeArguments = parameterList?.children?.any { it.type.toString() == "TYPE" } == true
    !hasTypeArguments && (classifier as? JavaClass)?.typeParameters?.isNotEmpty() == true
}
```

**2. JavaTypeConversion.kt** — Use `resolveTypeName()` to get FQN before raw type check:
```kotlin
val isRawType = isRaw || run {
    if (classifier != null || typeArguments.isNotEmpty()) false
    else if (mode == FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND) false
    else {
        // For unresolved types (star imports, java.lang), use resolve callback
        val resolvedClassId = resolveTypeName(classifierQualifiedName, this, session, source)
        val mappedClassId = JavaToKotlinClassMap.mapJavaToKotlin(resolvedClassId.asSingleFqName()) ?: resolvedClassId
        mappedClassId.toLookupTag().toRegularClassSymbol(session)?.typeParameterSymbols?.isNotEmpty() == true
    }
}
```

### Test Results
- **Phased tests**: 93 → 85 failures (**-8**)
- **Box tests**: 15 → 13 failures (**-2**)
- **Total improvement: 10 tests fixed**
- **Raw type tests**: 7 → 2 failures (**-5**)
- PSI regression tests: All passing ✅
- JavaParsingTest: All 42 tests passing ✅

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — Fixed `isRaw` detection
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` — Fixed raw type detection for unresolved types
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt` — Added unit tests for raw type detection

### Remaining Raw Type Failures (2)
- `testPseudoRawTypes` — Java compilation error (special test with custom `java.util.Collection`)
- `testRawSupertypeOverride` — Complex raw supertype inheritance scenario

### Key Learnings
1. **AST nodes can be empty but present** — Always check for actual children, not just node existence
2. **Debug with unit tests first** — Adding `testRawTypeDetection` to JavaParsingTest quickly revealed the empty REFERENCE_PARAMETER_LIST issue
3. **Two-level fix needed** — Java Model `isRaw` handles local classes, FIR handles external classes via resolution
4. **Star imports need resolution** — `classifierQualifiedName` returns simple names for star imports; must resolve before using

---

## Iteration 34: Type Parameter Identity Across Class Finder Lookups — 2026-03-16

### Status
✅ Complete — 3 tests fixed

### Root Cause Analysis

Tests `testInnerWithTypeParameter`, `testSeveralInnersWithTypeParameters`, and `testSupertypeInnerAndTypeParameterWithSameNames` all failed with:
```
SourceCodeAnalysisException: Cannot serialize error type: ERROR CLASS: Unresolved name: T
```

The error occurred during Kotlin metadata serialization, not Java model creation. FIR failed to substitute outer class type parameters (e.g. `T` from `x<T>`) in return types of inner class methods.

**Root cause**: `parseTopLevelClassFromFile` created a fresh `JavaClassOverAst` instance for the outer class every time it was called. This means:

1. When FIR loaded `a.x` via `findClass(ClassId("a", "x"))` → created `x1` with `T1: JavaTypeParameterOverAst`
2. When FIR loaded `a.x.y` via `findClass(ClassId("a", "x.y"))` → `findClasses` calls `parseTopLevelClassFromFile` again → created `x2` with `T2: JavaTypeParameterOverAst` → navigated to `y2` via `x2.findInnerClass("y")` → `y2`'s methods reference `T2`

FIR matches `JavaTypeParameter` by object identity when building FIR type parameter symbols. `T1 !== T2`, so FIR could not find `T2` in its scope → `ERROR CLASS: Unresolved name: T`.

The same pattern applies to `Outer<Foo>` where `Foo` is a type parameter name that shadows an inner class — FIR failed to resolve `Foo` as a type parameter.

### Fix

Changed `parseTopLevelClassFromFile` to accept `FileEntry` (which already holds `packageFqName`) and check the class cache before parsing:

```kotlin
private fun parseTopLevelClassFromFile(file: FileEntry, simpleName: String): JavaClassOverAst? {
    val classId = ClassId(file.packageFqName, FqName(simpleName), isLocal = false)
    classCache[classId]?.let { return it as? JavaClassOverAst }  // return cached instance

    // ... parse file, create JavaClassOverAst ...

    return JavaClassOverAst(node, resolutionContext, outerClass = null).also {
        classCache[classId] = it  // cache for future lookups
    }
}
```

This ensures that all lookups for a given top-level class — whether direct (`findClass("a.x")`) or as an intermediate step during inner-class navigation (`findClass("a.x.y")` internally creates `x`) — return the **same** `JavaClassOverAst` instance with the **same** `JavaTypeParameterOverAst` instances.

### Test Results
- **Phased tests**: +3 fixed (`testInnerWithTypeParameter`, `testSeveralInnersWithTypeParameters`, `testSupertypeInnerAndTypeParameterWithSameNames`)
- **Total failures**: 98 → 94
- PSI regression tests: All passing ✅

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` — Cache top-level class instances in `parseTopLevelClassFromFile`; accept `FileEntry` instead of `Path`

### Key Learnings
1. **FIR matches JavaTypeParameter by object identity** — Two `JavaTypeParameterOverAst` instances representing the same type parameter are not equal in FIR's eyes; only the exact same object works
2. **Separate class finder lookups can produce different instances** — `findClass("a.x")` and the intermediate x during `findClass("a.x.y")` both call `parseTopLevelClassFromFile` independently
3. **Error appears late in pipeline** — The mismatch only manifests during FIR metadata serialization (backend), not during Java model creation, making it harder to trace
4. **Cache top-level classes eagerly** — Caching in `parseTopLevelClassFromFile` at creation time (rather than only in `findClass`) ensures consistency even when the top-level class is an intermediate navigation step

---

## Iteration 35: Unresolvable Enum Annotation Argument Crash Fix — 2026-03-16

### Status
✅ Complete — 1 test fixed

### Root Cause Analysis

Test `testTestIllegalAnnotationClass` crashed with:
```
IllegalArgumentException: Required value was null.
  at javaAnnotationsMapping.kt:208 (requireNotNull call)
```

**Setup**: Java annotation `@State` in package `simulation` with `@Retention(RetentionPolicy.RUNTIME)` but **no import** for `java.lang.annotation.RetentionPolicy`.

**Flow**: When generating IR for `KotlinImporterComponent` (annotated with `@State`), the backend calls `getAnnotationTargets` on the `simulation.State` annotation class. This lazily evaluates `@Retention(RetentionPolicy.RUNTIME)` on the Java `State` class — a `JavaEnumValueAnnotationArgument`.

**Crash path** in `javaAnnotationsMapping.kt`:
1. `resolveEnumClass` tries to resolve `RetentionPolicy` → fails (not imported, not in `java.lang.*`, not in star imports) → returns `null`
2. First fallback: `expectedArrayElementTypeIfArray?.lowerBoundIfFlexible()?.classId` → also `null` (no expected type context when evaluating class-level Java annotations from IR)  
3. `requireNotNull(null)` → `IllegalArgumentException`

With PSI-based approach this never happens: PSI reads `@Retention` from the compiled `.class` file where `RetentionPolicy` is fully qualified as `java.lang.annotation.RetentionPolicy`.

### Fix

In `javaAnnotationsMapping.kt`, replaced `requireNotNull(...)` in the fallback else branch with an explicit null check:

```kotlin
val fallbackClassId = expectedArrayElementTypeIfArray?.lowerBoundIfFlexible()?.classId
if (fallbackClassId != null) {
    buildEnumEntryDeserializedAccessExpression {
        enumClassId = fallbackClassId
        enumEntryName = entryName ?: SpecialNames.NO_NAME_PROVIDED
    }
} else {
    buildErrorExpression {
        this.source = source
        diagnostic = ConeSimpleDiagnostic(
            "Cannot resolve enum annotation argument: ${entryName?.asString() ?: "?"}",
            DiagnosticKind.Java,
        )
    }
}
```

The error expression allows FIR to continue processing without crashing. The test baseline (`.fir.txt`) only covers the Kotlin file output, not the Java annotation class internals, so it matches correctly.

### Test Results
- **Phased tests**: +1 fixed (`testTestIllegalAnnotationClass` in `Resolve$Diagnostics`)
- **Total failures**: 94 → 93
- PSI regression tests: All passing ✅ (change is in shared FIR code, PSI never hits this null path)

### Files Modified
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/javaAnnotationsMapping.kt` — Replace `requireNotNull` with null-safe fallback to error expression

### Key Learnings
1. **Source-based Java parsing vs bytecode** — PSI reads compiled `.class` files where annotations are fully qualified; java-direct reads source where imports may be missing
2. **`requireNotNull` in shared FIR code can crash java-direct** — Shared code may have assumptions that hold for PSI (classpath always resolves) but not for source-based parsing
3. **Error expressions are better than crashes** — When an annotation argument can't be resolved, returning a `buildErrorExpression` is always preferable to throwing
