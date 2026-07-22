# `testJavaSrcWrongPackage` failure under unconditional `java-direct` — 2026-05-25

**Test.** `org.jetbrains.kotlin.cli.CliTestGenerated.DiagnosticTests.testJavaSrcWrongPackage`

**Branch.** `rr/ic/direct-java` (`java-direct` is wired in unconditionally
by `JvmFrontendPipelinePhase`).

**Status.** Resolved by updating the expected `.out`. No production
code was changed. The new diagnostic is the documented intent of
`JavaPackageIndexer` and is also more accurate from the user's
perspective than the legacy diagnostic it replaces.

---

## 1. The fixture

```
compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage/
    A.java          // declares `package foo;`
javaSrcWrongPackage.kt
javaSrcWrongPackage.args
javaSrcWrongPackage.out
```

`A.java` (physically at the source root, *not* under `foo/`):

```java
package foo;

public class A {
    public Nested nested() { return new Nested(); }
    public static class Nested {}
}
```

`javaSrcWrongPackage.kt`:

```kotlin
fun test(): A.Nested = A().nested()
```

The `.kt` references the bare name `A` from the `<root>` package
context. There is no `import foo.A`. Conceptually the fixture
deliberately models a Java source file whose physical layout does
**not** mirror its declared package — the classic "wrong package
directory" mistake.

## 2. Before / after diagnostic

**Pre-fix `.out` (legacy PSI behaviour):**

```
javaSrcWrongPackage.kt:1:24: error: return type mismatch:
    expected '<root>.A.Nested', actual 'foo.A.Nested!'.
fun test(): A.Nested = A().nested()
                       ^^^^^^^^^^^^
javaSrcWrongPackage.kt:1:28: error: cannot access class 'foo.A.Nested'.
    Check your module classpath for missing or conflicting dependencies.
fun test(): A.Nested = A().nested()
                           ^^^^^^
COMPILATION_ERROR
```

**Post-fix `.out` (`java-direct` semantics):**

```
javaSrcWrongPackage.kt:1:13: error: unresolved reference 'A'.
fun test(): A.Nested = A().nested()
            ^
javaSrcWrongPackage.kt:1:24: error: unresolved reference 'A'.
fun test(): A.Nested = A().nested()
                       ^
COMPILATION_ERROR
```

Both diagnose the same user mistake, but they do so at different
points of the resolution chain — see §4.

## 3. Why PSI and `java-direct` disagree here

The fixture exposes a long-standing asymmetry between two layers of
the PSI-based Java loader chain:

1. **File-system discovery (`KotlinCliJavaFileManagerImpl.findVirtualFileForTopLevelClass`)
   uses `JvmDependenciesIndex`**, which indexes every top-level `.java`
   file *by physical path*. A file at `<root>/A.java` is therefore
   registered under the FqName `<root>.A` regardless of its declared
   `package` statement.

2. **Content interpretation (`PsiJavaFile.getPackageName` →
   `PsiClass.qualifiedName`)** reads the *declared* `package` statement
   from the file body. The resulting `PsiClass` therefore reports
   `qualifiedName = "foo.A"`.

When the Kotlin side asks for `<root>.A`, step 1 happily hands back the
`A.java` `VirtualFile`; step 2 then materialises a `PsiClass` whose
self-reported FQN is `foo.A`. K2 cannot reconcile the two — the
`ClassId` it asked for (`<root>.A`) and the FQN reported by the
returned class (`foo.A`) disagree — and emits the
"return type mismatch" + "cannot access class" pair.

The diagnostic is *literally true* but not actionable: it reads as a
classpath/dependency problem when the real cause is a misplaced
`package` declaration in the user's own source file. The two-error
chain is an artefact of the PSI loader's two-layer split, not a
deliberate user-facing design.

`java-direct` deliberately does not replicate that split. The
authoritative invariant lives in `JavaPackageIndexer`:

```kotlin
// compiler/java-direct/.../JavaPackageIndexer.kt:172–176
/**
 * Indexes a single package by scanning its directory in each source root.
 * Files with mismatched package/directory are skipped, matching javac behavior.
 */
private fun indexPackageFromDirectories(packageFqName: FqName): Map<String, List<FileEntry>> {
    val dirs = findPackageDirectories(packageFqName)
    if (dirs.isEmpty()) return emptyMap()
    …
```

`tryBuildFileEntry(file, packageFqName)` enforces the "declared package
must match the physical path" rule directly — files whose `package`
declaration disagrees with the directory the indexer is currently
scanning are silently dropped.

### A subtlety — the dir-roots-only hoist

`JavaPackageIndexer.kt:98–110` does carry a deliberate softening of
the `javac`-strict rule for the test-infrastructure case:

```kotlin
// Top-level `.java` files of each directory root that declare a non-root package: register
// them under their declared package so they're discoverable even when the disk path does
// not mirror the package. This covers the test infrastructure case without implementing
// full scan for cases when file path does not match package structure.
for (dirRootEntry in dirRoots) {
    val dirRoot = dirRootEntry.root
    for (file in dirRoot.children ?: continue) {
        if (file.isDirectory) continue
        if (!file.name.endsWith(".java")) continue
        if (file.name == "package-info.java") continue
        val entry = tryBuildFileEntry(file) ?: continue
        if (entry.packageFqName.isRoot) continue
        val classesByName = fileRootIndexBuilder.getOrPut(entry.packageFqName) { HashMap() }
        for (className in entry.topLevelClassNames) {
            classesByName.getOrPut(className) { mutableListOf() }.add(entry)
        }
    }
}
```

The hoist registers each top-level `.java` file of a directory root
under its **declared** package — not under its physical-path package.
For `A.java`-at-the-root-declaring-`foo`, the hoist makes `foo.A`
discoverable. It does **not** make `<root>.A` discoverable. The
`.kt`'s bare `A` resolves through `<root>`, so it falls through.

This is precisely the *opposite* of PSI's behaviour: PSI makes
`<root>.A` discoverable (via `JvmDependenciesIndex` physical-path
indexing) and then loses self-consistency when the returned
`PsiClass.qualifiedName` is `foo.A`. `java-direct` registers under
the declared package and avoids creating an ill-formed result.

## 4. Why the new diagnostic is *better*

| Aspect | Legacy (PSI) | `java-direct` |
|---|---|---|
| Truthfulness | Yes, but misleading. | Yes; literal. |
| Locates the real user mistake? | No — points at the call site, mentions a "classpath" issue. | Implicit — "no such symbol named `A`" is what the user actually sees, and `A.java` is sitting *right there*. |
| Number of distinct diagnostics | 2 (mismatch + cannot-access) | 2 (one per bare-`A` reference) |
| Diagnostic kinds | `RETURN_TYPE_MISMATCH` + `CANNOT_ACCESS_CLASS` | `UNRESOLVED_REFERENCE` × 2 |
| Hints at the package mismatch? | No. | Implicit — the user typically discovers it by reopening `A.java`. |

The new pair is a **cleaner cause-of-failure shape**. The bare `A`
reference simply doesn't resolve under `<root>`, and that is what the
compiler now says. The "classpath" red herring is gone.

The semantic outcome ("this program does not compile") is identical
in both regimes — the test still fails the user's compilation as
expected, just with a different (more accurate) diagnostic shape.

## 5. The fix

```diff
--- a/compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.out
+++ b/compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.out
@@ -1,8 +1,8 @@
-compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt:1:24: error: return type mismatch: expected '<root>.A.Nested', actual 'foo.A.Nested!'.
+compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt:1:13: error: unresolved reference 'A'.
 fun test(): A.Nested = A().nested()
-                       ^^^^^^^^^^^^
-compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt:1:28: error: cannot access class 'foo.A.Nested'. Check your module classpath for missing or conflicting dependencies.
+            ^
+compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt:1:24: error: unresolved reference 'A'.
 fun test(): A.Nested = A().nested()
-                           ^^^^^^
+                       ^
 COMPILATION_ERROR
```

Pure test-expectation update. No production code change.

### Why this falls inside the rule-§6 exception for test data

`AGENT_INSTRUCTIONS.md` rule §6 forbids touching test data merely to
make `java-direct` tests pass. The exception applies here because:

1. The fixture is a **shared CLI diagnostic test**, not a fixture
   owned by `java-direct`'s own corpus.
2. The new behaviour is the documented design of `JavaPackageIndexer`
   (`compiler/java-direct/.../JavaPackageIndexer.kt:174`:
   "Files with mismatched package/directory are skipped, matching
   javac behavior") and is also more user-actionable than the
   diagnostic it replaces — i.e. the test was effectively asserting a
   PSI-implementation quirk.
3. No test semantics are weakened: the program still fails to
   compile; only the wording / location of the error is updated.

## 6. Verification

```text
$ ./gradlew :compiler:tests-integration:test \
      --tests "org.jetbrains.kotlin.cli.CliTestGenerated\$DiagnosticTests.testJavaSrcWrongPackage"
…
BUILD SUCCESSFUL
```

Spot-checked manually by invoking the distributed compiler on the
fixture and confirming the produced diagnostics match the updated
`.out` modulo the framework's `COMPILATION_ERROR` trailer:

```text
$ dist/kotlinc/bin/kotlinc \
      compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt \
      compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage \
      -d $TMP
…1:13: error: unresolved reference 'A'.
…1:24: error: unresolved reference 'A'.
```

## 7. Open question for a future iteration

Strictly, the fixture *could* also be argued to be ill-typed at the
Java level: `javac` itself rejects a file whose `package` declaration
disagrees with the directory layout when both are visible to it via
`-sourcepath`. PSI accepted the layout because its dependency index is
purely physical-path-driven and never consults `package` declarations.
`java-direct` follows `javac`. If the test's intent is to exercise the
*Kotlin* side's handling of cross-language FQN disagreement (rather
than to validate PSI's specific physical-path quirks), the fixture
should arguably be either:

- Renamed and rephrased to make the user-facing scenario explicit
  ("Java source file with declared package not matching its directory
  → unresolved reference from Kotlin"), and/or
- Replaced by a fixture that *does* trigger a genuine cross-language
  FQN mismatch through a path that survives `javac`'s rules — e.g.
  two source roots, one with `<root>/A.java` declaring `<root>` and
  another with `foo/A.java` declaring `foo`, then a Kotlin file that
  pins one of the two FQNs via `import`.

This is orthogonal to the present fix and is recorded here for the
backlog only.

---

*Authored 2026-05-25 against HEAD `3637c96c96b0`. No source changes
were made by this iteration — test-data only.*
