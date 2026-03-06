# Java-Direct: Agent Instructions

## Quick Reference

| Metric | Value |
|--------|-------|
| **Status** | Iteration 16 complete |
| **Box Tests** | 1075/1166 passing (92.2%) |
| **Phased Tests** | 242/327 passing (74.0%) |
| **Total** | 1317/1493 passing (88.2%) |

**Key files**: `JavaClassFinderOverAstImpl.kt`, `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`, `JavaResolutionContext.kt`

---

## Ground Rules

### Mandatory
- **Use JetBrains MCP tools** for all project file operations (see `.ai/guidelines.md`)
- **FIR terminology**: `simpleImports`/`starImports` (NOT singleType/onDemand)
- **Update ITERATION_RESULTS.md** after each iteration
- **Check files with `get_file_problems`** after changes

### Code Quality
- **No obvious comments** — comment only "why", never "what"
- **Minimal code** — solve the problem, don't over-engineer
- **Every fix needs a test** — unit test or box test must pass
- **Lazy evaluation** — avoid eager computation

---

## Key Architecture Decisions

### 1. Type Resolution in FIR Layer (Not Java Model)
Java Model provides names (`classifierQualifiedName`), FIR resolves them via `session.symbolProvider`. **No `FirSession` access in Java Model**.

### 2. Callback Pattern for Resolution
`resolve(tryResolve: (String) -> Boolean)` in `JavaClassifierType` allows Java Model to implement Java resolution rules while FIR validates existence. Same pattern used for annotations via `resolveAnnotation()`.

### 3. Hybrid Class Finder
`CombinedJavaClassFinder` tries source class finder first, falls back to binary class finder for JDK/library classes.

### 4. Resolution Context Pattern
`JavaResolutionContext` encapsulates all resolution data (package, imports, type parameters, containing class). Passed through AST nodes. Use `withTypeParameters()`, `withContainingClass()` to extend scope.

### 5. Two-Phase Type Parameter Construction
When type parameters can reference each other in bounds (e.g., `<E, S extends Element<E>>`):
1. Create all instances first with basic context
2. Update context with all siblings via `updateResolutionContext()`

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
Compare with PSI-based implementation in `compiler/frontend.java/src/org/jetbrains/kotlin/load/java/structure/impl/` or javac-based in `compiler/javac-wrapper/src/org/jetbrains/kotlin/javac/`.

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
| Raw type errors | Check `isRaw` detection, ensure `ConeRawType.create()` wrapping in FIR |
| Annotation not resolved | Use callback pattern via `resolveAnnotation(tryResolve)` |
| `@Override` on return type | Filter non-TYPE_USE annotations in `JavaTypeOverAst.annotations` |
| Nested interface wrong `isInner` | `isStatic` must return `true` for nested interfaces/enums |

### What NOT to Do
- **Don't hardcode package lists** for annotation resolution - use callback pattern
- **Don't modify FIR layer** unless java-direct model is correct but FIR handling is wrong
- **Don't assume AST structure** - always dump and verify with exception debugging
- **Don't skip empty ERROR_ELEMENT nodes** when scanning siblings

---

## Iteration Workflow

### 1. Run Tests
```bash
# Run box tests (1166 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" -q 2>&1 | tee test_output.txt

# Run phased/diagnostic tests (327 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" -q 2>&1 | tee test_output.txt

# Run a specific test
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated.testSpecificName" -q
```

### 2. Categorize Failures
```python
import xml.etree.ElementTree as ET
import glob
from collections import defaultdict

results_dir = "compiler/java-direct/build/test-results/test"
failures = defaultdict(list)

for xml_file in glob.glob(f"{results_dir}/*.xml"):
    tree = ET.parse(xml_file)
    for tc in tree.findall('.//testcase'):
        failure = tc.find('failure')
        if failure is not None:
            name = tc.get('name').replace('()','')
            text = (failure.text or '') + (failure.get('message', '') or '')
            if 'MISSING_DEPENDENCY_CLASS' in text:
                failures['MISSING_DEPENDENCY_CLASS'].append(name)
            # ... more patterns

for cat, tests in sorted(failures.items(), key=lambda x: -len(x[1])):
    print(f"\n=== {cat} ({len(tests)} tests) ===")
    for t in tests: print(f"  - {t}")
```

### 3. Debug, Fix, Verify
1. Add exception-based debugging
2. Implement fix
3. Verify single test passes
4. Run full suite to measure progress
5. Look through the test report and try to spot similar cases
6. If found, try to modify the fix to accommodate the similar cases

### 4. Document
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

**FIR integration** (may need changes for external type handling):
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
| `FIXING_ITERATIONS.md` | Current iteration plans and archive links |
| `IMPLEMENTATION_PLAN.md` | Architecture overview |
| `ITERATION_RESULTS.md` | Progress history, key findings |
| `implDocs/INVESTIGATION_TECHNIQUES.md` | Detailed debugging techniques |
| `implDocs/archive/` | Archived iteration details and design documents |

---

## Remaining Work (176 failing tests)

| Category | Count | Notes |
|----------|-------|-------|
| Box tests | 91 | Various runtime issues |
| Phased tests | 85 | Diagnostic/type checking edge cases |

---

*Last updated: 2026-03-06 (iteration 16 complete)*
