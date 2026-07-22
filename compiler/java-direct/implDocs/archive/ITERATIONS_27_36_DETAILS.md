# Java-Direct: Iterations 27-36 Details (Archived)

**Archive Date**: 2026-03-16
**Coverage**: Iterations 27 through 36
**Result**: 1150/1167 box → 1157/1168 box (99.1%), phased 1374/1442 → 1374/1442 (95.3%), combined 2610 tests, **79 failing**

> **Warning**: Archived for historical reference only. Consult if debugging regressions or understanding specific decisions.

---

## Iteration Summary

| Iteration | Date | Focus | Tests Fixed | Key Change |
|-----------|------|-------|-------------|------------|
| 27 | 2026-03-12 | Java Records (Java Model only) | +0 | `JavaRecordComponentOverAst`, `recordComponents` |
| 28 | 2026-03-13 | Java Records FIR Integration | +6 phased, +2 box | `RECORD` token, `isVararg`, canonical ctor detection |
| 29 | 2026-03-13 | Ambiguity Detection (same-file) | +2 phased | Set-based `findInnerClassFromSupertypes` |
| 30 | 2026-03-13 | Cross-file Ambiguity Detection | +2 phased | `classFinderProvider`, supertype walking |
| 31 | 2026-03-13 | JavaParsingTest Regressions Fix | +0 (regression fix) | Removed eager java.lang resolution |
| 32 | 2026-03-13 | Kotlin Constants in Java Annotations | +2 phased | Const field detection in `javaAnnotationsMapping.kt` |
| 33 | 2026-03-16 | Raw Types Detection | +8 phased, +2 box | `isRaw` fix, star-import FQN resolution |
| 34 | 2026-03-16 | Type Parameter Identity | +3 phased | Cache top-level class instances in class finder |
| 35 | 2026-03-16 | Enum Annotation Crash Fix | +1 phased | `requireNotNull` → `buildErrorExpression` |
| 36 | 2026-03-16 | Visibility + Enum Entries | +12 phased, +2 box | enumEntriesOrigin, enum const visibility, nested class visibility |

---

## Iteration 27: Java Records — Java Model

**Problem**: `recordComponents` returned empty, preventing record tests from passing.

**Fix**: Created `JavaRecordComponentOverAst.kt`, implemented `recordComponents` in `JavaClassOverAst` by finding `RECORD_HEADER` → `RECORD_COMPONENT` children.

**Outcome**: 0 tests fixed — Java Model alone is not enough; FIR must generate synthetic properties from record components.

---

## Iteration 28: Java Records — FIR Integration

**Three bugs**:

1. **`RECORD_KEYWORD` token mismatch** (primary): `isRecord` checked `findChildByType("RECORD_KEYWORD")` but parser produces token name `"RECORD"` (like `SEALED` vs `"SEALED_KEYWORD"`). Fixed: `findChildByType("RECORD")`.

2. **`isVararg` incomplete**: Only checked component node directly, not inside the TYPE child. Fixed to also check `node.findChildByType("TYPE")?.findChildByType("ELLIPSIS")`.

3. **Canonical constructor detection**: `isPrimary` used PSI-based `JavaPsiRecordUtil.isCanonicalConstructor` — returns false for java-direct (no PSI). Fixed in `FirJavaFacade.kt`: for source-based records, compare constructor parameter names with record component names (JLS guarantees same names).

**Key learning**: Parser token `toString()` differs from constant name for RECORD and SEALED — always verify via sources or AST dump.

---

## Iteration 29: Ambiguity Detection (Same-File)

**Problem**: When multiple supertypes have inner classes with the same name, javac reports ambiguity. Java-direct returned the first match.

**Fix**: Changed `findInnerClassFromSupertypes()` and `resolveFromSupertypes()` in `JavaResolutionContext.kt` to collect ALL matches into a `Set`, return `null` if size > 1.

**Fixed**: `testInheritanceAmbiguity`, `testInheritanceAmbiguity3`.

---

## Iteration 30: Cross-File Ambiguity Detection

**Problem**: `testInheritanceAmbiguity2` and `testInheritanceAmbiguity4` require detecting ambiguity when conflicting inner classes come from supertypes defined in *different files*.

**Fix**: Added `classFinderProvider` callback to `JavaResolutionContext`. When local ambiguity check finds nothing, walk the supertype chain across files by parsing just the `extends`/`implements` clauses. `collectInheritedInnerClasses()` with cycle detection finds all candidate names.

**Bug discovered**: Initial `fqNameToClassId()` used uppercase-heuristic to split package from class name — failed for lowercase class names (`x`, `y`). Fixed by using `packageFqName` from context directly.

**All 4 ambiguity tests pass** after this iteration.

---

## Iteration 31: JavaParsingTest Regressions Fix

**Problem**: Cross-file ambiguity changes inadvertently triggered a latent inconsistency: `JAVA_LANG_TYPES` map in `classifierQualifiedName` returned `"java.lang.Object"` (qualified) for unresolved `"Object"`, but `isResolved` returned `false`. Unit tests expected unqualified names for unresolved types.

**Fix**:
- Removed `JAVA_LANG_TYPES` eager resolution from `classifierQualifiedName` (returns raw source name)
- Updated `hasConstantNotNullInitializer` to accept both `"String"` and `"java.lang.String"`
- Fixed `testLocalInheritance` expectation (now expects implicit `java.lang.Object` supertype)

**Key learning**: `classifierQualifiedName` and `isResolved` must be consistent — unresolved types keep raw names.

---

## Iteration 32: Kotlin Constants in Java Annotations

**Problem**: Java annotations using Kotlin `const val` (e.g., `@Foo(KotlinClass.FOO_INT)`) failed to resolve.

**Fix**:
- Reused `JavaEnumValueAnnotationArgument` for both enum AND const field references
- In `javaAnnotationsMapping.kt`: detect if resolved symbol is a const property (check class declarations + companion object), extract `FirLiteralExpression.value`
- Targeted fix in `JavaAnnotationOverAst.resolveEnumClass()` for default-package case

**Key learning**: Broad changes to name resolution cause cascading regressions (99 failures in one attempt). Targeted fixes in specific code paths are safer.

---

## Iteration 33: Raw Types Detection

**Two bugs**:

**Bug 1 — `isRaw` in JavaTypeOverAst.kt**: `REFERENCE_PARAMETER_LIST` node exists even when empty → old check `findChildByType(...) != null` always `true` for any reference type → `isRaw` always `false`. Fixed: check for actual TYPE children inside the list.

```kotlin
// Before: existence check (always true even for empty list)
val hasParameterList = node.findChildByType("REFERENCE_PARAMETER_LIST") != null
// After: content check
val hasTypeArguments = parameterList?.children?.any { it.type.toString() == "TYPE" } == true
```

**Bug 2 — FIR raw type check for star-import types**: `classifierQualifiedName` returns simple name `"List"` for star imports. `ClassId.topLevel(FqName("List"))` creates wrong ClassId. Fixed: resolve via `resolveTypeName()` callback to get FQN before checking type parameters.

**Key learning**: AST nodes can be empty but still present — always check children, not just node existence.

---

## Iteration 34: Type Parameter Identity Across Lookups

**Problem**: `testInnerWithTypeParameter` etc. failed with `ERROR CLASS: Unresolved name: T` during FIR metadata serialization.

**Root cause**: `parseTopLevelClassFromFile` created a fresh `JavaClassOverAst` every call. `findClass("a.x")` and the intermediate `x` during `findClass("a.x.y")` produced different instances with different `JavaTypeParameterOverAst` objects for `T`. FIR matches type parameters by object identity — `T1 !== T2` → unresolved.

**Fix**: Accept `FileEntry` (has `packageFqName`) in `parseTopLevelClassFromFile`, check class cache before parsing:
```kotlin
val classId = ClassId(file.packageFqName, FqName(simpleName), isLocal = false)
classCache[classId]?.let { return it as? JavaClassOverAst }
// ... parse and create ...
return JavaClassOverAst(...).also { classCache[classId] = it }
```

**Key learning**: FIR matches JavaTypeParameter by object reference — must return the same instance across all lookups for a given class.

---

## Iteration 35: Enum Annotation Crash Fix

**Problem**: `IllegalArgumentException: Required value was null` in `javaAnnotationsMapping.kt` when processing `@Retention(RetentionPolicy.RUNTIME)` on a Java annotation class that has no import for `RetentionPolicy`.

**Root cause**: Both resolution paths returned null (unresolved class + no expected type context) → `requireNotNull(null)` → crash.

**Fix**: Replace `requireNotNull` with explicit null check → return `buildErrorExpression` with `DiagnosticKind.Java` when both paths fail.

**Key learning**: `requireNotNull` in shared FIR code assumes PSI always resolves — source-based parsing (java-direct) may not have all imports available.

---

## Iteration 36: Visibility + Enum Entries

**Three bugs** sharing the theme "implicit Java modifier not implemented":

**Bug 1 — Java enum `.entries` (NoSuchFieldError at runtime)**:
`enumEntriesOrigin = Java.Source` for java-direct. `Java.Source` is `FirDeclarationOrigin.Java` → `isJavaOrigin=true` → no getter symbol, backing field `entries` created → `GETSTATIC MyEnum.entries` → `NoSuchFieldError`.
`Source` blocked because `FirPropertyAccessorImpl` requires source element.
Fix: use `Library` for java-direct (non-Java, no source element required) → getter created → `EnumExternalEntriesLowering` intrinsifies correctly.

**Bug 2 — Enum constant visibility (INVISIBLE_REFERENCE)**:
`ENUM_CONSTANT` nodes have no explicit modifiers → fell through to `PackageVisibility`. JLS 8.9.3: enum constants are implicitly `public static final`.
Fix: `override val visibility` in `JavaFieldOverAst` returns `Public` when `isEnumEntry`.

**Bug 3 — Nested class visibility (INVISIBLE_REFERENCE)**:
`JavaClassOverAst.visibility` missing two cases:
- JLS 9.5: nested types in interfaces → `Public` (missing `outerClass?.isInterface == true` check)
- Protected nested classes → `ProtectedAndPackage`/`ProtectedStaticVisibility` (returned plain `Protected` which excludes same-package access)

**Key learning**: When any implicit-Java-rule fix is needed, audit the *entire* file against the reference implementation — all similar cases in the same file likely have the same gap.

---

## Files Modified (Iterations 27-36)

| File | Iterations | Changes |
|------|-----------|---------|
| `JavaClassOverAst.kt` | 28, 36 | `RECORD` token, visibility fixes (interface nested → Public, protected → ProtectedAndPackage) |
| `JavaMemberOverAst.kt` | 36 | Enum constant visibility override |
| `JavaRecordComponentOverAst.kt` | 27, 28 | New file: record component Java Model |
| `JavaTypeOverAst.kt` | 31, 33 | Removed eager java.lang, fixed `isRaw` detection |
| `JavaResolutionContext.kt` | 29, 30 | Ambiguity detection (same-file + cross-file) |
| `JavaClassFinderOverAstImpl.kt` | 30, 34 | Cross-file supertype parsing, top-level class cache |
| `JavaAnnotationOverAst.kt` | 32 | Default-package const resolution |
| `JavaParsingTest.kt` | 31, 33 | Regression fixes and new raw type tests |
| `FirJavaFacade.kt` (shared) | 28, 36 | Canonical ctor detection, `enumEntriesOrigin` fix |
| `JavaTypeConversion.kt` (shared) | 33 | Star-import FQN resolution for raw type check |
| `javaAnnotationsMapping.kt` (shared) | 32, 35 | Const field detection, crash fix for unresolvable enums |

---

## Test Results Progression

| After Iteration | Box Tests | Phased Tests | Combined Failing |
|----------------|-----------|--------------|-----------------|
| 27 | 1150/1167 | 1341/1442 | ~125 |
| 28 | 1152/1167 | 1347/1442 | ~110 |
| 29 | 1152/1167 | 1349/1442 | ~108 |
| 30 | 1152/1167 | 1351/1442 | ~106 |
| 31 | 1152/1167 | 1351/1442 | ~106 (regression fix) |
| 32 | 1152/1167 | 1353/1442 | ~104 |
| 33 | 1155/1168 | 1362/1442 | ~93 |
| 34 | 1155/1168 | 1365/1442 | ~93 (XML stale, actual ~90) |
| 35 | 1155/1168 | 1366/1442 | ~89 |
| 36 | 1157/1168 | 1374/1442 | **79** |

---

*Archived: 2026-03-16*
