# IntelliJFullPipelineTestsGenerated — Regression Analysis (java-direct vs master)

**Date:** 2026-05-10
**Inputs:**
- `report-2026-05-08__16-34.direct.log` — CI run on `rr/ic/direct-java`
- `report-2026-05-08__23-12.main.log` — CI run on `master`

**Topline:** master = 48 failed modules (4337 OK / 4385). java-direct = 59 failed
modules (4326 OK / 4385). The 11-module delta is exclusively new failures on
java-direct; no module regressed in the opposite direction.

## Modules failing only on java-direct

| Module                              | First-error code                                | Symbol involved                                      |
|-------------------------------------|-------------------------------------------------|------------------------------------------------------|
| intellij.android.core               | `MISSING_DEPENDENCY_SUPERCLASS`                 | `BaseBuilder` of `StudioExceptionReport.Builder`     |
| intellij.android.lint.common        | `ABSTRACT_MEMBER_NOT_IMPLEMENTED`               | `setPriority(PriorityAction.Priority)`               |
| intellij.android.transport          | JVM `NegativeArraySizeException` (ASM bytegen)  | —                                                    |
| intellij.bigdatatools.zeppelin      | `UNRESOLVED_IMPORT/REFERENCE`                   | `ScalaLibraryProperties$`, `Element$`, `None$`, `package$` |
| intellij.javascript.psi.impl        | `ABSTRACT_MEMBER_NOT_IMPLEMENTED`               | `JSRecordType.MemberSource` / `PropertySignature`    |
| intellij.javascript.tests           | `ABSTRACT_MEMBER_NOT_IMPLEMENTED` + `CANNOT_WEAKEN_ACCESS_PRIVILEGE` | TypeScript* hierarchies                              |
| intellij.platform.debugger.impl     | `UNRESOLVED_REFERENCE_WRONG_RECEIVER`           | `XLineBreakpointType.XLineBreakpointVariant<*>.asProxy` |
| intellij.platform.lang.impl         | `MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR` | `NlsContexts.Tooltip` (already-known, see ITERATION_RESULTS.md) |
| intellij.r                          | `ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED`         | `RowFilter.Entry<...>`                               |
| intellij.remoteRun                  | JVM `NegativeArraySizeException` (ASM bytegen)  | —                                                    |
| intellij.swift.language             | `ABSTRACT_MEMBER_NOT_IMPLEMENTED` (~30 classes) | `SwiftSymbolResult<T>` hierarchy                     |

## Local repro

Selectively reran the 11 tests on `rr/ic/direct-java`:

```bash
./gradlew :compiler:fir:modularized-tests:test \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_android_core" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_android_lint_common" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_android_transport" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_bigdatatools_zeppelin" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_javascript_psi_impl" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_javascript_tests" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_platform_debugger_impl" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_platform_lang_impl" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_r" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_remoteRun" \
  --tests "*IntelliJFullPipelineTestsGenerated.testIntellij_swift_language" \
  --stacktrace --rerun
```

Result: **10 failed, 1 passed** — `testIntellij_android_transport` passed
locally despite failing in the CI report (intermittent — same test method also
showed `NegativeArraySizeException` in the CI failure list, suggesting a
non-deterministic codegen crash).

## Note on activation

`-Xjava-direct` is **not** required for these failures. Java-direct's binary
class finder is unconditionally registered:

- `JvmConfigurationPipelinePhase.kt:94-96` — gating `if` is commented out;
  `JavaDirectPluginRegistrar` is added on every CLI compile.
- `JavaDirectPluginRegistrar.kt:88` — `USE_BINARY_FINDER = true` (hard-coded).

So every modularized-tests run exercises the new
`BinaryJavaClassFinder` + `CombinedJavaClassFinder` path, regardless of
`args.javaDirect`. The XML model dumps don't carry `javaDirect=true` and the
test harness only forwards `originalArguments.javaDirect` from the XML
(`AbstractIsolatedFullPipelineModularizedTest.kt:118`); none of those XMLs
set it. The regressions therefore live in code paths active by default.

## Categories

### A. Inherited nested class from Java supertype invisible to Kotlin subclass

**Modules:** android.lint.common, javascript.psi.impl, javascript.tests,
swift.language, r, android.core (6 of 11).

**Symptom:** every case is a Kotlin class extending a Java class whose
**abstract** members reference a nested type declared on a *transitive* Java
supertype (e.g. `RowFilter.Entry`, `JSRecordType.MemberSource`,
`PriorityAction.Priority`, `SwiftSymbolResult`,
`StudioExceptionReport.Builder` ↘ `BaseBuilder`). FIR sees the supertype's
abstract method but cannot resolve the nested type referenced in its
signature, so the Kotlin subclass appears not to implement the abstract
member.

**Root-cause hypothesis:** the recent
`findInheritedNestedClass` double-guard fix (2026-05-08, see
`ITERATION_RESULTS.md` head entry) addressed only the **Java-source**
resolution path in `JavaResolutionContext.kt:107`. The same shape over
**binary** supertype walks — used when FIR loads members from `.class`/`.sig`
— probably starves the loop checker the same way. The shared
`JavaSupertypeLoopChecker` keys by classId alone; any binary-side entry
point that enters the guard for `outerClassId`, then asks for supertypes via
`directSupertypeClassIds(outerClassId)`, hits the re-entry check and gets
`emptyList()`.

**Investigation steps:**
1. Instrument `JavaInheritedMemberResolver.walkBinarySupertypes`
   (`JavaInheritedMemberResolver.kt:229`) and
   `JavaResolutionContext.directSupertypeClassIds`
   (`JavaResolutionContext.kt:195`) — log entry/exit and the active set of
   the loop checker — running `testIntellij_android_lint_common`
   (smallest, 50 files; canonical example: `PriorityAction.Priority`).
2. Check whether some FIR Java loading path (`FirJavaFacade`,
   `JavaTypeConversion.toConeKotlinTypeForFlexibleBound`'s `findClassId`
   probe) bypasses the per-origin `directSupertypeClassIds` dispatcher and
   ends up walking the same shared loop checker from a re-entrant context.

### B. `$`-named top-level class invisible from binary classpath

**Module:** intellij.bigdatatools.zeppelin
(`ScalaLibraryProperties$`, `Element$`, `None$`, `package$`).

**Root cause — confirmed by code inspection:**
`BinaryJavaClassFinder.knownClassNamesInPackage`
(`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/BinaryJavaClassFinder.kt:184-199`)
excludes any class file whose name contains `$`:

```kotlin
override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
    knownClassNamesCache.getOrPut(packageFqName) {
        val result = LinkedHashSet<String>()
        index.traverseClassVirtualFilesInPackage(packageFqName, extensions) { file ->
            val name = file.nameWithoutExtension
            if (!name.contains('$')) {     // <-- bug
                result.add(name)
            }
            true
        }
        result
    }
```

PSI's equivalent
(`KotlinCliJavaFileManagerImpl.knownClassNamesInPackage`,
`compiler/cli/cli-base/src/org/jetbrains/kotlin/cli/jvm/compiler/KotlinCliJavaFileManagerImpl.kt:267-280`)
adds **every** class file's name, no `$` filter:

```kotlin
override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> {
    val result = ObjectOpenHashSet<String>()
    index.traverseClassVirtualFilesInPackage(packageFqName, RELEVANT_JAVA_FILE_EXTENSIONS) { file ->
        result.add(file.nameWithoutExtension)
        true
    }
    ...
}
```

Scala companion-module classes (`X$.class`) are legitimate top-level classes
on the JVM. Kotlin imports them via backticks
(`import org.jetbrains.plugins.scala.project.\`ScalaLibraryProperties$\``
in `ScalaSdkDependencyPatcherImpl.kt:11`). The filter hides them from
`knownClassNamesInPackage`, so FIR's package-known-names gate refuses the
lookup before ever calling `findClass` — even though `findClassImpl`'s own
`isNotTopLevelClass(classContent)` guard at line 141 would have correctly
admitted these specific files (they are top-level on disk).

**Fix:** mirror PSI — drop the `$` filter in `knownClassNamesInPackage`. The
existing `isNotTopLevelClass` guard inside `findClassImpl` is the right
place for the inner-class-spillover defence; doing it at the package
enumeration step is too coarse.

**Cross-check:** `testIntellij_bigdatatools_zeppelin` only.
JavaUsingAst\* matrix unaffected (those tests don't exercise `$`-named
binary classes).

### C. Generic receiver mismatch on Java nested generic type

**Module:** intellij.platform.debugger.impl.
`fun XLineBreakpointType.XLineBreakpointVariant<*>.asProxy(): XLineBreakpointInlineVariantProxy.Monolith`
— extension on a Java nested generic class with star projection. Java-direct
likely renders the receiver type slightly differently from PSI (raw vs star;
owner reference; type-parameter-stack handling), so the receiver match fails
at the call site.

**Investigation step:** dump `JavaClassifierType` representation for
`XLineBreakpointType.XLineBreakpointVariant<*>` in both paths;
compare with `git show origin/master:.../JavaTypeConversion.kt`. Likely
culprit: `JavaTypeConversion.toConeKotlinTypeForFlexibleBound` or
`MutableJavaTypeParameterStack`'s nested-class handling.

### D. Cross-module inferred-type annotation accessibility (already known)

**Module:** intellij.platform.lang.impl (`NlsContexts.Tooltip`).
Recorded in `ITERATION_RESULTS.md` head entry; different category from A.

### E. ASM bytegen crash (`NegativeArraySizeException` at `Frame.merge`)

**Modules:** intellij.remoteRun (reproduced locally),
intellij.android.transport (CI-only; passed locally — likely intermittent).

A negative-sized array allocation in `org.jetbrains.org.objectweb.asm.Frame.merge`
during stack-frame merge is a textbook downstream symptom of a malformed
signature reaching codegen — usually generic substitution producing a type
with a missing or inconsistent component. Fixing categories A/C is likely
to clear this without a direct codegen change.

**Investigation step (after A/C):** if still reproducing, rerun with
`-Xverify-ir=error` to surface the inconsistency before ASM hits it.

## Recommended order

1. **B (this iteration):** one-line filter removal in
   `BinaryJavaClassFinder.knownClassNamesInPackage`. Single test to verify
   (`testIntellij_bigdatatools_zeppelin`). Zero risk to the JavaUsingAst\*
   matrix.
2. **A (next):** instrument `testIntellij_android_lint_common` and confirm
   the binary-side analogue of the `findInheritedNestedClass` double-guard
   bug. If confirmed, the same hoist-supertype-out-of-guard pattern (or a
   loop-checker key extension to `(entry-point, classId)`) likely fixes
   3–5 of the 6 modules in this category.
3. **D, E (verify after A):** likely downstream and may clear without
   further work.
4. **C (last):** localise the `XLineBreakpointVariant<*>` receiver
   divergence with a typeRef dump.

## Logs

- Local rerun output: `compiler/fir/modularized-tests/build/test-results/test/TEST-org.jetbrains.kotlin.fir.IntelliJFullPipelineTestsGenerated.xml`
- Session temp: `/tmp/jd_20260510_100011/` — `direct_compile_failed.txt`,
  `main_compile_failed.txt`, `ij_fp.txt`.
