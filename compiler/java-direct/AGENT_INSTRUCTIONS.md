# Java-Direct: Agent Instructions

## Quick Reference

| Metric | Value |
|--------|-------|
| **Status** | Iteration 23 complete |
| **Box Tests** | 1134/1166 passing (97.2%) |
| **Phased Tests** | 300/329 passing (91.2%) |
| **Remaining** | ~62 failing tests |

**Key files**: `JavaClassFinderOverAstImpl.kt`, `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`, `JavaResolutionContext.kt`

---

## Ground Rules

### Mandatory
- **Use JetBrains MCP tools** for all project file operations (see `.ai/guidelines.md`)
- **FIR terminology**: `simpleImports`/`starImports` (NOT singleType/onDemand)
- **Update ITERATION_RESULTS.md** after each iteration
- **Check files with `get_file_problems`** after changes
- **Run PSI tests after FIR changes** — see "Shared vs Java-Direct Files" section

### Code Quality
- **No obvious comments** — comment only "why", never "what"
- **Minimal code** — solve the problem, don't over-engineer
- **Every fix needs a test** — unit test or box test must pass
- **Lazy evaluation** — avoid eager computation
- **Prefer callbacks over hardcoded lists** — see "Callback Pattern" section

---

## Key Architecture Decisions

### 1. Type Resolution in FIR Layer (Not Java Model)
Java Model provides names (`classifierQualifiedName`), FIR resolves them via `session.symbolProvider`. **No `FirSession` access in Java Model**.

### 2. Callback Pattern for Resolution

**CRITICAL**: Always prefer callback-based resolution over hardcoded lists.

`resolve(tryResolve: (String) -> Boolean)` in `JavaClassifierType` allows Java Model to implement Java resolution rules while FIR validates existence.

**Established callback patterns** (use these as templates for new features):

| Feature | Interface Method | FIR Callback |
|---------|-----------------|--------------|
| Type resolution | `JavaClassifierType.resolve(tryResolve)` | `symbolProvider.getClassLikeSymbolByClassId` |
| Annotation resolution | `JavaAnnotation.resolveAnnotation(tryResolve)` | `symbolProvider.getClassLikeSymbolByClassId` |
| Enum class resolution | `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve)` | `findClassId()` in `JavaTypeConversion.kt` |
| TYPE_USE filtering | `JavaType.filterTypeUseAnnotations(isTypeUse)` | `isTypeUseAnnotationClass()` |
| Constant evaluation | `JavaField.resolveInitializerValue(resolveReference)` | `resolveExternalFieldValue()` in `FirJavaFacade.kt` |

**Why callbacks**: Allows java-direct to handle its own resolution without affecting PSI-based or javac-wrapper implementations.

### 3. Hybrid Class Finder
`CombinedJavaClassFinder` tries source class finder first, falls back to binary class finder for JDK/library classes.

### 4. Resolution Context Pattern
`JavaResolutionContext` encapsulates all resolution data (package, imports, type parameters, containing class). Passed through AST nodes. Use `withTypeParameters()`, `withContainingClass()` to extend scope.

### 5. Two-Phase Type Parameter Construction
When type parameters can reference each other in bounds (e.g., `<E, S extends Element<E>>`):
1. Create all instances first with basic context
2. Update context with all siblings via `updateResolutionContext()`

---

## Shared vs Java-Direct Files

**CRITICAL**: Some files are shared between java-direct and PSI-based class finders. Changes to these files may break PSI tests.

### Java-Direct Specific (safe to modify)
- `compiler/java-direct/src/**` — All java-direct implementation files
- `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`, etc.

### Shared FIR Files (modify with caution)
- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` — Used by all Java class finders
- `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` — Type conversion for all Java sources
- `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt` — Annotation handling
- `core/compiler.common.jvm/src/.../load/java/structure/*.kt` — Java model interfaces

### After Modifying Shared Files
**Always run PSI regression tests**:
```bash
./gradlew :compiler:fir:fir-jvm:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" -q
```

### Discriminating Java-Direct from PSI
When shared FIR code needs different behavior for java-direct:
```kotlin
// Java-direct classes have null source (no PSI)
val isJavaDirectClass = classSource == null && origin.fromSource
```

---

## Proven Debugging Techniques

### Exception-Based Inspection
**Why**: println/logging doesn't appear in Gradle test output.

```kotlin
throw IllegalStateException("DEBUG: value='$value', context='$context'")
```

**Conditional** (debug specific cases only):
```kotlin
if (classId.shortClassName.asString() == "TargetClass") {
    throw IllegalStateException("DEBUG: classId=$classId")
}
```

### AST Structure Discovery
```kotlin
if (typeNode.text.contains("[")) {
    throw IllegalStateException("DEBUG: typeNode.dump=\n${typeNode.dump()}")
}
```

### Comparing with Working Implementation
Compare with PSI-based implementation in `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/java/structure/impl/` or javac-based in `compiler/javac-wrapper/src/org/jetbrains/kotlin/javac/`.

### Finding Test Data Files
Test names map to files in `compiler/testData/`. Use MCP tools to find them:
```kotlin
// Example: testJavaAnnotation → 
mcp__jetbrains__find_files_by_name_keyword("javaAnnotation", ...)
// Then read the test file to understand the exact scenario
```

---

## Lessons Learned from Iterations

### Java Language Implicit Rules
- **Interface fields**: implicitly `public static final`
- **Interface methods without body**: implicitly `public abstract`
- **Nested interfaces/enums**: implicitly `static` (even without keyword)
- **Nested classes**: only static if explicitly marked

### KMP Parser Edge Cases
- **Reserved words in imports**: `import kotlin.*` may parse as `ERROR_ELEMENT`, not `IMPORT_STATEMENT`
- **Fragmented imports**: Parser may split constructs across sibling nodes
- **Recovery needed**: `ERROR_ELEMENT` nodes often contain recoverable info

### FIR Integration Points
- **Type conversion**: `JavaTypeConversion.kt` - handles `classifier==null` for external types
- **Raw types**: Must create `ConeRawType` for proper method inheritance semantics
- **Flexible types**: Two calls - lower bound with erased args, upper bound with star projections
- **TYPE_USE annotations**: Annotations from method modifier list need filtering before attaching to return type

### Common Fixes
| Issue | Solution |
|-------|----------|
| `MISSING_DEPENDENCY_CLASS: 'T'` | Type parameter not in scope - use `resolutionContext.withTypeParameters()` |
| `MISSING_DEPENDENCY_CLASS: 'Outer.Inner'` | Nested class in binary - need to resolve outer first, then lookup nested |
| Raw type errors | Check `isRaw` detection, ensure `ConeRawType.create()` wrapping in FIR |
| Annotation not resolved | Use callback pattern via `resolveAnnotation(tryResolve)` |
| `IR annotation has null argument` (literal) | `JavaAnnotationArgument` must implement value subinterfaces (Literal/Array/Enum/etc) |
| `IR annotation has null argument` (const val) | `REFERENCE_EXPRESSION` for const val needs special handling, not enum |
| `UNRESOLVED_REFERENCE: 'value'` on annotation | Annotation INTERFACE methods need to be exposed (not annotation argument issue) |
| `@Override` on return type | Filter non-TYPE_USE annotations in `JavaTypeOverAst.annotations` |
| Nested interface wrong `isInner` | `isStatic` must return `true` for nested interfaces/enums |
| Nullability check fails | TYPE_USE annotations on type arguments need parsing |

### What NOT to Do
- **Don't hardcode lists** for resolution/filtering - use callback pattern instead
- **Don't modify shared FIR files** without running PSI regression tests
- **Don't assume AST structure** - always dump and verify with exception debugging
- **Don't skip empty ERROR_ELEMENT nodes** when scanning siblings
- **Don't implement partial interfaces** - e.g., `JavaAnnotationArgument` requires value subinterfaces (`JavaLiteralAnnotationArgument`, etc.), not just `name`
- **Don't estimate fix counts without debugging** - categorize by symptom first, verify root cause before estimating

---

## Iteration Workflow

### Recommended Approach: Ad-Hoc Debugging

For complex/interconnected issues (like remaining ~62 failures), use the ad-hoc approach that proved effective in iterations 11-16:

1. **Run tests and categorize** — Group by error message pattern
2. **Debug representative test** — Pick 2-3 tests from largest category, add exception debugging
3. **Verify root cause** — Ensure all tests in category have SAME root cause (not just same symptom)
4. **Implement and verify** — Fix, run single test, run full suite
5. **Look for similar cases** — Check if fix helps other categories
6. **Document findings** — Update `ITERATION_RESULTS.md`

### Run Tests
```bash
# Run box tests (1166 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" -q 2>&1 | tee test_output.txt

# Run phased/diagnostic tests (329 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" -q 2>&1 | tee test_output.txt

# Run a specific test
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated.testSpecificName" -q

# IMPORTANT: After modifying shared FIR files, verify PSI tests
./gradlew :compiler:fir:fir-jvm:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.Resolve.Multiplatform.*" -q
```

### Categorize Failures

**Important**: Test results are in nested XML files. Use recursive glob:

```python
import xml.etree.ElementTree as ET
import glob
from collections import defaultdict
import re

results_dir = "compiler/java-direct/build/test-results/test"
failures = defaultdict(list)

def categorize(text):
    """Extract error category from failure text"""
    patterns = [
        (r'MISSING_DEPENDENCY_CLASS', 'MISSING_DEP_CLASS'),
        (r'MISSING_DEPENDENCY_SUPERCLASS', 'MISSING_DEP_SUPER'),
        (r'IR annotation has null argument', 'ANNOTATION_NULL_ARG'),
        (r"UNRESOLVED_REFERENCE.*value", 'ANNOTATION_VALUE_UNRESOLVED'),
        (r'NoSuchMethodError', 'NO_SUCH_METHOD'),
        (r'INVISIBLE_REFERENCE', 'INVISIBLE_REF'),
        (r'ABSTRACT_MEMBER_NOT_IMPLEMENTED', 'ABSTRACT_NOT_IMPL'),
        (r'Actual data differs from file content', 'BASELINE_DIFF'),
        (r'Content is not equal', 'CONTENT_DIFF'),
        (r'should throw on get\(\)', 'NULLABILITY_CHECK_FAIL'),
        (r'There should left no projections', 'STAR_PROJECTION_BUG'),
    ]
    for pattern, cat in patterns:
        if re.search(pattern, text, re.IGNORECASE):
            return cat
    return 'OTHER'

# Use recursive glob to find all XML files (tests are in nested directories)
for xml_file in glob.glob(f"{results_dir}/**/*.xml", recursive=True):
    try:
        tree = ET.parse(xml_file)
        for tc in tree.findall('.//testcase'):
            failure = tc.find('failure')
            if failure is not None:
                name = tc.get('name').replace('()','')
                classname = tc.get('classname', '')
                text = (failure.text or '') + (failure.get('message', '') or '')
                test_type = 'box' if 'Box' in classname else 'phased'
                cat = categorize(text)
                failures[f"{test_type}:{cat}"].append((name, text[:200]))
    except: pass

for key in sorted(failures.keys(), key=lambda k: -len(failures[k])):
    tests = failures[key]
    print(f"\n=== {key} ({len(tests)} tests) ===")
    for name, text in tests[:5]:
        print(f"  - {name}")
```

### Before Implementing: Root Cause Verification

**CRITICAL**: Same error message does NOT mean same root cause.

Example from Iteration 17: "UNRESOLVED_REFERENCE: 'value'" had THREE different root causes:
1. Annotation argument values not parsed (fixed by annotation subinterfaces)
2. Annotation method access (annotation interface methods not exposed)
3. Const val references (REFERENCE_EXPRESSION incorrectly treated as enum)

**Verify root cause by debugging 2-3 tests** before estimating fix impact.

### Document
Update `ITERATION_RESULTS.md` with findings, changes, and test counts.

---

## Key Files

| File | Purpose |
|------|---------|
| `JavaClassFinderOverAstImpl.kt` | Source class finder, file indexing |
| `JavaClassOverAst.kt` | Java class model, `memberResolutionContext` |
| `JavaTypeOverAst.kt` | Type representations, `classifierQualifiedName`, wildcards |
| `JavaMemberOverAst.kt` | Methods, fields, parameters |
| `JavaResolutionContext.kt` | Import/type parameter scope management |
| `JavaAnnotationOverAst.kt` | Annotation parsing and resolution |
| `JavaDirectComponentRegistrar.kt` | Plugin registration, hybrid finder setup |

**Reference implementations** (check these BEFORE implementing new features):

| Feature | javac-wrapper | PSI-based |
|---------|---------------|-----------|
| Type resolution | `TreeBasedClassifierType` | `JavaClassifierTypeImpl` |
| Annotation args | `TreeBasedAnnotation` | `annotationArgumentsImpl.kt` |
| Supertypes | `TreeBasedClass.supertypes` | `JavaClassImpl.supertypes` |
| Type arguments | `TreeBasedClassifierType.typeArguments` | `JavaClassifierTypeImpl.typeArguments` |

**Paths**:
- javac-wrapper: `compiler/javac-wrapper/src/org/jetbrains/kotlin/javac/wrappers/`
- PSI-based: `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/java/structure/impl/`

**FIR integration** (shared files - modify with caution):
- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` — Java class → FIR conversion
- `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` — type conversion, raw type detection
- `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt` — annotation resolution

---

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Circular FIR↔Java dependency | Use lazy evaluation |
| Wrong terminology | Use FIR names (simpleImports, starImports) |
| Using wrong tools | Use MCP tools, not Read/Edit/Grep |
| Type params not in scope | Use `withTypeParameters()` before parsing bounds |
| External type args dropped | Check FIR's `null` classifier branch in `JavaTypeConversion.kt` |

---

## Testing

### Test Classes
- **`JavaUsingAstBoxTestGenerated`** — Box tests (runtime behavior verification)
- **`JavaUsingAstPhasedTestGenerated`** — Phased/diagnostic tests (compilation diagnostics)
- **`JavaParsingTest`** — Custom unit tests for precise coverage of specific parsing features

### Adding Custom Tests
For precise coverage of a fix or feature, add tests to `JavaParsingTest` in `testFixtures/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt`. This is useful when:
- Testing specific AST parsing behavior
- Verifying edge cases not covered by generated tests
- Quick iteration during development

---

## Related Documents

| Document | Purpose |
|----------|---------|
| `FIXING_ITERATIONS.md` | Future iteration plans |
| `IMPLEMENTATION_PLAN.md` | Architecture overview |
| `ITERATION_RESULTS.md` | Progress history, key findings |
| `PROCESS_ANALYSIS.md` | Development process analysis and recommendations |
| `implDocs/archive/` | Archived iteration details and design documents |

---

## Remaining Work (~62 failing tests)

After iteration 23, remaining failures fall into these categories:

| Category | Est. Count | Notes |
|----------|-----------|-------|
| Baseline diffs (may be correct) | ~20 | Need individual triage |
| Annotation edge cases | ~10 | Const val refs, special types |
| Visibility/access issues | ~5 | Protected field patterns |
| Reflection/metadata | ~5 | May require FIR changes |
| Modern Java features | ~5 | Records, sealed classes |
| Other edge cases | ~17 | Need individual analysis |

### Approach for Remaining Work

Use **ad-hoc debugging approach** (proven effective in iterations 11-16):
1. Run tests and categorize by error message
2. Debug 2-3 representative tests to verify root cause
3. Implement fix targeting verified root cause
4. Run PSI regression tests if FIR files modified

**Do NOT** create detailed upfront plans for complex issues - the remaining failures are interconnected and reveal themselves during debugging.

---

*Last updated: 2026-03-12 (iteration 23 complete)*
