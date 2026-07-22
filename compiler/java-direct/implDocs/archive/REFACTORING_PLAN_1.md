# Java-Direct Module: Refactoring Plan

This document synthesizes findings from three independent code reviews (r1, r2, r3) into a detailed, step-by-step refactoring plan. Changes are organized into testable chunks: first architectural improvements, then performance optimizations, then remaining code quality fixes.

---

## Part 1: Architectural Changes

### Step 1.1 — Replace String-Based AST Type Comparisons with `SyntaxElementType` Constants

**Problem**: ~30+ call sites across all source files compare AST node types via `it.type.toString() == "IDENTIFIER"` etc. This creates unnecessary `String` allocations on every comparison and is fragile (typos silently fail, no compile-time verification).

**Available constants**: The `com.intellij.java.syntax.element` package provides:
- `JavaSyntaxElementType` — composite node types: `CLASS`, `FIELD`, `METHOD`, `ANNOTATION_METHOD`, `ENUM_CONSTANT`, `MODIFIER_LIST`, `TYPE`, `TYPE_PARAMETER`, `TYPE_PARAMETER_LIST`, `JAVA_CODE_REFERENCE`, `REFERENCE_PARAMETER_LIST`, `EXTENDS_LIST`, `IMPLEMENTS_LIST`, `PERMITS_LIST`, `EXTENDS_BOUND_LIST`, `IMPORT_LIST`, `IMPORT_STATEMENT`, `IMPORT_STATIC_STATEMENT`, `IMPORT_STATIC_REFERENCE`, `PACKAGE_STATEMENT`, `ANNOTATION`, `ANNOTATION_PARAMETER_LIST`, `NAME_VALUE_PAIR`, `ANNOTATION_ARRAY_INITIALIZER`, `LITERAL_EXPRESSION`, `REFERENCE_EXPRESSION`, `BINARY_EXPRESSION`, `PREFIX_EXPRESSION`, `POLYADIC_EXPRESSION`, `CONDITIONAL_EXPRESSION`, `PARENTH_EXPRESSION`, `TYPE_CAST_EXPRESSION`, `CLASS_OBJECT_ACCESS_EXPRESSION`, `ARRAY_INITIALIZER_EXPRESSION`, `RECORD_COMPONENT`, `RECORD_HEADER`, `PARAMETER`, `PARAMETER_LIST`, `THROWS_LIST`, `CODE_BLOCK` (lazy), `DIAMOND_TYPE`, etc.
- `JavaSyntaxTokenType` — leaf tokens: `IDENTIFIER`, `AT`, `DOT`, `COMMA`, `SEMICOLON`, `EQ`, `ASTERISK`, `LBRACKET`, `RBRACKET`, `LBRACE`, `RBRACE`, `LPARENTH`, `RPARENTH`, `QUEST`, `ELLIPSIS`, `PLUS`, `MINUS`, `EXCL`, `TILDE`, `PERC`, `DIV`, `AND`, `OR`, `XOR`, `ANDAND`, `OROR`, `LTLT`, `GTGT`, `GTGTGT`, `LT`, `GT`, `LE`, `GE`, `EQEQ`, `NE`, all `*_KEYWORD` tokens (`PUBLIC_KEYWORD`, `PRIVATE_KEYWORD`, `PROTECTED_KEYWORD`, `STATIC_KEYWORD`, `FINAL_KEYWORD`, `ABSTRACT_KEYWORD`, `DEFAULT_KEYWORD`, `SYNCHRONIZED_KEYWORD`, `VOLATILE_KEYWORD`, `TRANSIENT_KEYWORD`, `NATIVE_KEYWORD`, `STRICTFP_KEYWORD`, `SEALED_KEYWORD`, `NON_SEALED_KEYWORD`, `ENUM_KEYWORD`, `INTERFACE_KEYWORD`, `CLASS_KEYWORD`, `RECORD_KEYWORD`, `EXTENDS_KEYWORD`, `IMPLEMENTS_KEYWORD`, `VOID_KEYWORD`, `BOOLEAN_KEYWORD`, `BYTE_KEYWORD`, `SHORT_KEYWORD`, `INT_KEYWORD`, `LONG_KEYWORD`, `CHAR_KEYWORD`, `FLOAT_KEYWORD`, `DOUBLE_KEYWORD`, `NULL_KEYWORD`, `TRUE_KEYWORD`, `FALSE_KEYWORD`, etc.), and literal tokens (`INTEGER_LITERAL`, `LONG_LITERAL`, `FLOAT_LITERAL`, `DOUBLE_LITERAL`, `CHARACTER_LITERAL`, `STRING_LITERAL`, `TEXT_BLOCK_LITERAL`).
- `SyntaxElementTypes` — useful sets: `MODIFIER_BIT_SET`, `KEYWORD_BIT_SET`, `PRIMITIVE_TYPE_BIT_SET`, `EXPRESSION_BIT_SET`, `OPERATION_BIT_SET`, `ANNOTATION_MEMBER_VALUE_BIT_SET`.
- `SyntaxTokenTypes` (platform) — `ERROR_ELEMENT`, `WHITE_SPACE`.

**Changes required**:

1. **`utils.kt`**: Deprecate or remove the string-based `findChildByType(typeName: String)` and `getChildrenByType(typeName: String)` overloads (lines 91-97). Keep only the `SyntaxElementType`-based overloads (lines 99-105). Update `computeTypeParameters` (line 111-112) to use `JavaSyntaxElementType.TYPE_PARAMETER_LIST` and `JavaSyntaxElementType.TYPE_PARAMETER`.

2. **`JavaClassOverAst.kt`** (~15 call sites): Replace all string comparisons:
   - `"IDENTIFIER"` → `JavaSyntaxTokenType.IDENTIFIER`
   - `"CLASS"` → `JavaSyntaxElementType.CLASS`
   - `"MODIFIER_LIST"` → `JavaSyntaxElementType.MODIFIER_LIST`
   - `"STATIC_KEYWORD"` → `JavaSyntaxTokenType.STATIC_KEYWORD`
   - `"JAVA_CODE_REFERENCE"` → `JavaSyntaxElementType.JAVA_CODE_REFERENCE`
   - `"AT"` → `JavaSyntaxTokenType.AT`
   - `"ENUM_KEYWORD"` → `JavaSyntaxTokenType.ENUM_KEYWORD`
   - `"INTERFACE_KEYWORD"` → `JavaSyntaxTokenType.INTERFACE_KEYWORD`
   - `"RECORD_KEYWORD"` → `JavaSyntaxTokenType.RECORD_KEYWORD`
   - `"EXTENDS_LIST"` → `JavaSyntaxElementType.EXTENDS_LIST`
   - `"IMPLEMENTS_LIST"` → `JavaSyntaxElementType.IMPLEMENTS_LIST`
   - `"PERMITS_LIST"` → `JavaSyntaxElementType.PERMITS_LIST`
   - `"TYPE_PARAMETER_LIST"` → `JavaSyntaxElementType.TYPE_PARAMETER_LIST`
   - `"RECORD_HEADER"` → `JavaSyntaxElementType.RECORD_HEADER`
   - `"RECORD_COMPONENT"` → `JavaSyntaxElementType.RECORD_COMPONENT`
   - etc.

3. **`JavaMemberOverAst.kt`** (~12 call sites): Replace:
   - `"ENUM_CONSTANT"` → `JavaSyntaxElementType.ENUM_CONSTANT`
   - `"FIELD"` → `JavaSyntaxElementType.FIELD`
   - `"METHOD"` / `"ANNOTATION_METHOD"` → `JavaSyntaxElementType.METHOD` / `JavaSyntaxElementType.ANNOTATION_METHOD`
   - `"TYPE"` → `JavaSyntaxElementType.TYPE`
   - `"EQ"` → `JavaSyntaxTokenType.EQ`
   - `"DEFAULT_KEYWORD"` → `JavaSyntaxTokenType.DEFAULT_KEYWORD`
   - `"PARAMETER_LIST"` → `JavaSyntaxElementType.PARAMETER_LIST`
   - `"PARAMETER"` → `JavaSyntaxElementType.PARAMETER`
   - `"THROWS_LIST"` → `JavaSyntaxElementType.THROWS_LIST`
   - `"ELLIPSIS"` → `JavaSyntaxTokenType.ELLIPSIS`
   - modifier keywords → corresponding `JavaSyntaxTokenType.*_KEYWORD`

4. **`JavaTypeOverAst.kt`** (~12 call sites): Replace:
   - `"TYPE"` → `JavaSyntaxElementType.TYPE`
   - `"LBRACKET"` → `JavaSyntaxTokenType.LBRACKET`
   - `"JAVA_CODE_REFERENCE"` → `JavaSyntaxElementType.JAVA_CODE_REFERENCE`
   - `"REFERENCE_PARAMETER_LIST"` → `JavaSyntaxElementType.REFERENCE_PARAMETER_LIST`
   - `"DIAMOND_TYPE"` → `JavaSyntaxElementType.DIAMOND_TYPE`
   - `"QUEST"` → `JavaSyntaxTokenType.QUEST`
   - `"EXTENDS_BOUND_LIST"` → `JavaSyntaxElementType.EXTENDS_BOUND_LIST`
   - `"ANNOTATION"` → `JavaSyntaxElementType.ANNOTATION`
   - primitive keyword strings → `JavaSyntaxTokenType.*_KEYWORD`

5. **`JavaAnnotationOverAst.kt`** (~6 call sites): Replace:
   - `"ANNOTATION_PARAMETER_LIST"` → `JavaSyntaxElementType.ANNOTATION_PARAMETER_LIST`
   - `"NAME_VALUE_PAIR"` → `JavaSyntaxElementType.NAME_VALUE_PAIR`
   - `"ANNOTATION_ARRAY_INITIALIZER"` → `JavaSyntaxElementType.ANNOTATION_ARRAY_INITIALIZER`
   - `"LITERAL_EXPRESSION"` → `JavaSyntaxElementType.LITERAL_EXPRESSION`
   - `"REFERENCE_EXPRESSION"` → `JavaSyntaxElementType.REFERENCE_EXPRESSION`
   - literal token types → `JavaSyntaxTokenType.INTEGER_LITERAL`, etc.

6. **`JavaResolutionContext.kt`** (~8 call sites): Replace:
   - `"ASTERISK"` → `JavaSyntaxTokenType.ASTERISK`
   - `"IDENTIFIER"` → `JavaSyntaxTokenType.IDENTIFIER`
   - `"CLASS"` → `JavaSyntaxElementType.CLASS`
   - `"IMPORT_LIST"` → `JavaSyntaxElementType.IMPORT_LIST`
   - `"IMPORT_STATEMENT"` → `JavaSyntaxElementType.IMPORT_STATEMENT`
   - `"IMPORT_STATIC_STATEMENT"` → `JavaSyntaxElementType.IMPORT_STATIC_STATEMENT`
   - `"DOT"` → `JavaSyntaxTokenType.DOT`
   - `"IMPORT_KEYWORD"` → `JavaSyntaxTokenType.IMPORT_KEYWORD`
   - `"STATIC_KEYWORD"` → `JavaSyntaxTokenType.STATIC_KEYWORD`
   - `"ERROR_ELEMENT"` → `SyntaxTokenTypes.ERROR_ELEMENT` (from `com.intellij.platform.syntax.element`)

7. **`JavaClassFinderOverAstImpl.kt`** (~5 call sites): Replace:
   - `"CLASS"` → `JavaSyntaxElementType.CLASS`
   - `"PACKAGE_STATEMENT"` → `JavaSyntaxElementType.PACKAGE_STATEMENT`
   - `"IDENTIFIER"` → `JavaSyntaxTokenType.IDENTIFIER`
   - `"DOT"` → `JavaSyntaxTokenType.DOT`

8. **`ConstantEvaluator.kt`** (~5 call sites): Replace:
   - `"CLASS"` → `JavaSyntaxElementType.CLASS`
   - `"LITERAL_EXPRESSION"` → `JavaSyntaxElementType.LITERAL_EXPRESSION`
   - `"REFERENCE_EXPRESSION"` → `JavaSyntaxElementType.REFERENCE_EXPRESSION`
   - `"BINARY_EXPRESSION"` → `JavaSyntaxElementType.BINARY_EXPRESSION`
   - `"PREFIX_EXPRESSION"` → `JavaSyntaxElementType.PREFIX_EXPRESSION`
   - `"PARENTH_EXPRESSION"` → `JavaSyntaxElementType.PARENTH_EXPRESSION`
   - `"TYPE_CAST_EXPRESSION"` → `JavaSyntaxElementType.TYPE_CAST_EXPRESSION`
   - `"CONDITIONAL_EXPRESSION"` → `JavaSyntaxElementType.CONDITIONAL_EXPRESSION`
   - `"POLYADIC_EXPRESSION"` → `JavaSyntaxElementType.POLYADIC_EXPRESSION`
   - operator tokens → `JavaSyntaxTokenType.PLUS`, `MINUS`, `ASTERISK`, `DIV`, `PERC`, `LTLT`, `GTGT`, `GTGTGT`, `AND`, `OR`, `XOR`, `EXCL`, `TILDE`, `EQEQ`, `NE`, `LT`, `GT`, `LE`, `GE`, `ANDAND`, `OROR`
   - literal tokens → `JavaSyntaxTokenType.INTEGER_LITERAL`, `LONG_LITERAL`, `FLOAT_LITERAL`, `DOUBLE_LITERAL`, `STRING_LITERAL`, `CHARACTER_LITERAL`, `TRUE_KEYWORD`, `FALSE_KEYWORD`, `NULL_KEYWORD`

9. **`JavaRecordComponentOverAst.kt`** (~4 call sites): Replace string-based calls similarly.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass). No behavioral change expected — purely mechanical replacement.

---

### Step 1.2 — Split `JavaResolutionContext` into Focused Collaborators

**Problem**: `JavaResolutionContext.kt` is 1,011 lines and acts as a "God class" combining import management, type parameter scoping, local class lookup, inherited inner class resolution, nested class resolution, simple name resolution, callback-based resolution, containing class chain management, cross-file ambiguity detection, and cache management. All three reviews identify this as the primary maintainability risk.

**Proposed decomposition**:

1. **`JavaImportResolver.kt`** — Extract import extraction and lookup:
   - `extractImports()` companion method (lines ~854-998) → becomes a top-level function or a dedicated class
   - Import data structures: `explicitImports: Map<String, FqName>`, `starImports: List<FqName>`, `staticImports`, `staticStarImports`
   - Import lookup methods: the parts of `resolve()` that probe explicit imports, star imports, java.lang, same-package

2. **`JavaScopeResolver.kt`** — Extract type parameter and local class scoping:
   - `findLocalClass()` logic (lines ~93-126) with containing class chain traversal
   - Type parameter lookup: `ownTypeParameters`, `inheritedTypeParameters`, `allTypeParameters`
   - `withTypeParameters()`, `withContainingClass()` factory methods

3. **`JavaInheritedMemberResolver.kt`** — Extract supertype hierarchy traversal for inner classes:
   - `findInnerClassFromSupertypes()` (lines ~138-190)
   - `resolveInheritedInnerClassToClassId()` with BFS (lines ~587-687)
   - `aggregatedInheritedInnerClasses` computation
   - Related caches: `findLocalClassCache`, `aggregatedInheritedInnerClassesHolder`

4. **`JavaResolutionContext.kt`** — Remains as orchestrator:
   - Composes the above three collaborators
   - `resolve()` method delegates to import resolver, scope resolver, inherited member resolver in JLS priority order
   - `resolveAsClassId()`, `resolveNestedClassToClassId()` remain here as they orchestrate across collaborators
   - Context creation factory methods

**Approach**: Start by extracting data and pure functions first (imports extraction, type parameter data). Then extract the inherited inner class resolution (most self-contained). Finally extract the scope/local-class logic. Keep `resolve()` in the orchestrator.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass) after each extraction. Behavior must be identical.

---

### Step 1.3 — Extract Duplicate Literal Parsing into Shared Utility

**Problem**: ~200 lines of identical code exist in both `JavaAnnotationOverAst.kt` (lines ~131-312) and `ConstantEvaluator.kt` (lines ~290-377): `parseIntegerLiteral`, `parseLongLiteral`, `parseFloatLiteral`, `parseDoubleLiteral`, `unescapeJavaString`, and related helpers.

**Changes**:

1. Create **`JavaLiteralParser.kt`** — a utility object containing:
   - `parseIntegerLiteral(text: String): Int?`
   - `parseLongLiteral(text: String): Long?`
   - `parseFloatLiteral(text: String): Float?`
   - `parseDoubleLiteral(text: String): Double?`
   - `unescapeJavaString(text: String): String`
   - Any shared helper functions (e.g., `stripUnderscores`, hex/octal/binary parsing)

2. Update `JavaAnnotationOverAst.kt` to delegate to `JavaLiteralParser`.

3. Update `ConstantEvaluator.kt` to delegate to `JavaLiteralParser`.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass). No behavioral change.

---

### Step 1.4 — Investigate ConstantEvaluator vs FIR Expression Evaluation

**Problem**: `ConstantEvaluator.kt` (380 lines) implements Java constant expression evaluation (literals, binary/unary operations, field references). FIR has its own `FirExpressionEvaluator` in `compiler/fir/providers/`. The question is whether the java-direct `ConstantEvaluator` can be replaced by the FIR-based one for Java-only parts.

**Investigation steps**:

1. Check what `FirExpressionEvaluator.evaluateExpression()` supports — it operates on `FirExpression` nodes, not raw Java AST. The java-direct `ConstantEvaluator` operates on `JavaSyntaxNode` (pre-FIR).

2. Check the call chain: `JavaField.resolveInitializerValue(resolveReference)` → `ConstantEvaluator.evaluate()`. This is called during Java model construction, before FIR conversion.

3. **Likely conclusion**: The `ConstantEvaluator` cannot be directly replaced by `FirExpressionEvaluator` because it runs at the Java model layer (pre-FIR). However, the evaluation could potentially be deferred to FIR if the raw expression AST is preserved and converted to FIR expressions first. This would require significant architectural changes to the constant evaluation pipeline.

4. **Recommendation**: Document the finding. If deferral to FIR is feasible, create a separate follow-up task. For now, keep `ConstantEvaluator` but ensure it shares literal parsing with `JavaLiteralParser` (Step 1.3).

**Verification**: Document-only step unless changes are made.

---

### Step 1.5 — Handle File I/O via External Service and Fix Error Swallowing

**Problem**: `JavaClassFinderOverAstImpl.kt` uses `tryReadFile()` (line ~413) which silently swallows `IOException` and returns `null`. Also uses `Files.walk()` and `path.toFile().readText()` for direct filesystem access. All three reviews flag this as a quality and diagnostic concern.

**Changes**:

1. **Introduce a `JavaSourceFileReader` interface**:
   ```kotlin
   interface JavaSourceFileReader {
       fun readFileContent(path: Path): CharSequence?
       fun walkSourceRoots(roots: List<Path>): Sequence<Path>
   }
   ```

2. **Default implementation** uses `VirtualFileSystem` or direct I/O with proper error reporting:
   - Log warnings on `IOException` instead of silently returning `null`
   - Distinguish between "file not found" (return `null`) and "file unreadable" (log warning + return `null`)
   - Use a proper logging framework or compiler diagnostic reporter

3. **Update `JavaClassFinderOverAstImpl`**:
   - Accept `JavaSourceFileReader` as a constructor parameter
   - Replace `tryReadFile()` with `reader.readFileContent()`
   - Replace `Files.walk()` in `buildIndex()` with `reader.walkSourceRoots()`

4. **Fix the TODO comment** at line ~413: remove `"shoulbe"` typo, document the error handling strategy.

5. **Update `JavaDirectComponentRegistrar`** to wire the reader.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass). Verify that I/O errors are now logged.

---

### Step 1.6 — Split `JavaClassFinderOverAstImpl` into Focused Components

**Problem**: `JavaClassFinderOverAstImpl.kt` (641 lines) combines filesystem walking, source index building, lightweight scanning, full parsing, class caching, package annotation indexing, supertype analysis, and inherited inner class collection. All three reviews recommend decomposition.

**Proposed decomposition**:

1. **`JavaSourceIndex.kt`** — Source file index builder:
   - `buildIndex()` logic (lines ~259-278)
   - Lightweight scanning for large files
   - Package-to-files mapping
   - `isClassInIndex()` fast-path check
   - `knownClassNamesInPackage()`

2. **`JavaFileParserCache.kt`** — File parser and class cache:
   - `parseTopLevelClassFromFile()` (eager and lazy paths)
   - `classCache: ConcurrentHashMap<ClassId, JavaClassOverAst>`
   - `packageCache`
   - Parse result caching at file granularity

3. **`JavaSupertypeGraph.kt`** — Supertype and inherited inner class support:
   - `getDirectSupertypes()` (lines ~461-490)
   - `collectInheritedInnerClasses()` (lines ~525-559)
   - `supertypeCache`, `inheritedInnerClassesCache`

4. **`JavaClassFinderOverAstImpl.kt`** — Remains as the `JavaClassFinder` implementation:
   - Composes the above three components
   - Delegates `findClass()`, `findPackage()`, `knownClassNamesInPackage()` to appropriate components

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass) after each extraction.

---

### Step 1.7 — Fix Remaining Architectural Code Smells

Address code smells identified in the reviews that fall within the refactored code:

1. **`CombinedJavaClassFinder.kt`**: Address the suspicious FQN verification logic (line ~38) with the `TODO` questioning the reasoning. Either validate and document the reasoning, or remove if unnecessary.

2. **`JavaClassOverAst.kt`**: Clarify the relationship between `findInnerClassInSupertypes` (lines ~170-202) and `JavaResolutionContext.findInnerClassFromSupertypes` (lines ~138-190). Document the distinction or unify if possible.

3. **`JavaTypeOverAst.kt`**: Make `updateResolutionContext` on `JavaTypeParameterOverAst` (line ~532-539) `internal` and add documentation about the two-phase construction invariant. Consider using `lateinit var` to make the contract explicit.

4. **`JavaDirectComponentRegistrar.kt`**: Remove the debug-log `TODO` (line ~63) — either implement proper logging or remove the temporary property.

5. **Fix typos** in comments: `"shoulbe"` → `"should be"`, `"enore"` → `"enough"`, `"reasonin"` → `"reasoning"`.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass).

---

## Part 2: Performance Optimizations

### Step 2.1 — Optimize `findChildByType` / `getChildrenByType` for Hot Nodes

**Problem**: Both methods perform linear scans over `children` lists. Multiple calls on the same node (finding MODIFIER_LIST, then IDENTIFIER, then TYPE_PARAMETER_LIST) result in O(n²) behavior for nodes with many children.

**Changes**:

1. For frequently accessed node types on hot paths (class nodes, method nodes), consider adding a lazily-built type-indexed map:
   ```kotlin
   // In JavaSyntaxNode or as an extension
   val childByType: Map<SyntaxElementType, JavaSyntaxNode> by lazy {
       children.associateBy { it.type }  // first child of each type
   }
   ```

2. Alternatively, for class/method nodes that are accessed many times, cache the commonly needed children (identifier, modifier list, type node) as lazy properties in the model classes themselves.

3. Measure before and after to confirm improvement.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass) + measure performance on a representative project.

---

### Step 2.2 — Optimize Import Extraction

**Problem**: `extractImports()` (145 lines) always scans all root children for fragmented imports, even when none exist. The fragmented import loop (lines ~927-995) iterates over ALL root children for every file.

**Changes**:

1. Add early exit: if `importList` is non-null and contains all expected imports, skip the fragmented import scan.
2. Gate the fragmented import scan behind a "has ERROR_ELEMENT" check — only scan if the file has parse errors.
3. Cache file-level import data per `JavaSyntaxNode` root to avoid redundant extraction when multiple contexts are created for the same file.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass).

---

### Step 2.3 — Optimize `collectInheritedInnerClasses` Recursive Traversal

**Problem**: The recursive `collectRecursive` within `collectInheritedInnerClasses` doesn't check the `inheritedInnerClassesCache` for intermediate results, causing redundant traversal in diamond inheritance patterns.

**Changes**:

1. Add a cache check at the start of `collectRecursive`:
   ```kotlin
   fun collectRecursive(current: ClassId, shadowedNames: Set<String>) {
       if (current in visited) return
       inheritedInnerClassesCache[current]?.let { cached ->
           for ((name, classIds) in cached) {
               if (name !in shadowedNames) {
                   result.getOrPut(name) { mutableSetOf() }.addAll(classIds)
               }
           }
           return
       }
       // ... existing logic
   }
   ```

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass).

---

### Step 2.4 — Add `textEquals` Method to Reduce String Allocations

**Problem**: `JavaSyntaxNode.text` creates a new `String` via `source.subSequence(startOffset, endOffset).toString()`. Many uses only need comparison, not the actual string.

**Changes**:

1. Add to `JavaSyntaxNode`:
   ```kotlin
   fun textEquals(expected: String): Boolean {
       if (endOffset - startOffset != expected.length) return false
       for (i in expected.indices) {
           if (source[startOffset + i] != expected[i]) return false
       }
       return true
   }
   ```

2. Replace `node.text == "someString"` patterns with `node.textEquals("someString")` where applicable (identifier comparisons, keyword checks).

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass).

---

### Step 2.5 — Optimize Negative Lookups and Resolution Caching

**Problem**: `CombinedJavaClassFinder` delegates to the AST source finder first; classes not in source incur full source-level fallback before binary fallback. Resolution probes in `JavaResolutionContext.resolve()` may repeatedly try the same missing `ClassId`.

**Changes**:

1. Add a negative lookup cache in `CombinedJavaClassFinder` for `ClassId`s confirmed absent from source.
2. Ensure the per-invocation `tryResolve` cache in `JavaResolutionContext.resolve()` also caches negative results (already partially done — verify completeness).
3. Consider a Bloom filter or `HashSet<ClassId>` for fast "definitely not in source" checks.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass) + measure resolution performance.

---

### Step 2.6 — Evaluate `ConcurrentHashMap` vs `HashMap` Usage

**Problem**: Several maps use `ConcurrentHashMap` even though `buildIndex()` runs single-threaded in `init`. If the module is single-threaded (FIR phase), the concurrent overhead is unnecessary.

**Changes**:

1. Verify with the FIR team whether multi-threaded access is required.
2. If single-threaded: switch `classCache`, `packageCache`, `supertypeCache`, `inheritedInnerClassesCache` to `HashMap`.
3. If multi-threaded: document why, and also fix `JavaClassOverAst.innerClassCache` (uses `HashMap` inconsistently — line ~118).

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass).

---

### Step 2.7 — Consider Richer Lightweight Index

**Problem**: The lightweight index only extracts package and top-level names. Later operations may need full parsing for modest metadata queries (supertype names, nested types, annotation presence).

**Changes** (lower priority, may be deferred):

1. Extend lightweight scanning to capture:
   - Direct supertype textual names
   - Presence and names of top-level nested declarations
   - Package-info/package annotation markers
2. This would let the class finder answer more questions without escalating to full parsing.
3. Measure the trade-off: scanning cost vs. avoided full-parse cost.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass) + benchmark on large projects.

---

## Part 3: Code Quality Improvements

### Step 3.1 — Improve `extractImports` Method Readability

**Problem**: The 145-line `extractImports` method handles 4 different import patterns in a single method with nested loops.

**Changes**:

1. Extract each import pattern into a separate private method:
   - `extractNormalImports(importList: JavaSyntaxNode)`
   - `extractStaticImports(importList: JavaSyntaxNode)`
   - `extractErrorElementImports(root: JavaSyntaxNode)`
   - `extractFragmentedImports(root: JavaSyntaxNode)`
2. Add unit tests for each pattern individually.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass).

---

### Step 3.2 — Document and Test Fragile Invariants

**Problem**: Several critical invariants depend on comments rather than structural enforcement.

**Changes**:

1. **Object identity for type parameters**: `parseTopLevelClassFromFile()` relies on reusing the same `JavaClassOverAst` instance. Add a dedicated test that verifies type-parameter identity is preserved across lookups.

2. **Nested class vs package name ambiguity**: Add tests for the JLS 6.5.2 rule (nested class interpretation takes priority over package interpretation).

3. **Cross-file inherited inner class behavior**: Add tests for diamond inheritance patterns and deep hierarchies.

4. **Canonical visibility for non-canonical top-level classes**: Add tests verifying that non-public top-level classes in files not matching their name are handled correctly.

**Verification**: New tests should pass.

---

### Step 3.3 — Address Remaining TODOs

**Problem**: Several TODOs indicate unfinished work or questionable logic.

**Changes**:

1. `JavaClassFinderOverAstImpl.kt:241` — `TODO: this is the place there we can fix KT-4455`: Investigate and either implement the fix or convert to a tracked issue.

2. `JavaClassFinderOverAstImpl.kt:485` — `TODO: check if this is rare enough`: Add a counter/log to verify the slow path frequency. If rare, document. If frequent, fix the caching gap.

3. `CombinedJavaClassFinder.kt:38` — `TODO: recheck this place, the reasoning is suspicious`: Validate the FQN verification logic. Either confirm and document, or fix.

4. `JavaDirectComponentRegistrar.kt:63` — `TODO: remove after testing or find a better way to debuglog`: Either implement proper logging via the compiler's diagnostic infrastructure, or remove the debug property entirely.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass) after each TODO resolution.

---

### Step 3.4 — Improve `isAnnotationType` Detection Robustness

**Problem**: `JavaClassOverAst.isAnnotationType` (line ~205) checks for `AT` token presence in direct children, which could be fragile.

**Changes**:

1. Add a comment explaining why this works with the KMP parser's specific AST structure for `@interface`.
2. Alternatively, check for the `AT` token immediately preceding the `INTERFACE_KEYWORD` token in the children list for a more robust check.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass).

---

### Step 3.5 — Consistent Cache Type Usage

**Problem**: `JavaClassOverAst.innerClassCache` uses `HashMap` while `JavaClassFinderOverAstImpl` caches use `ConcurrentHashMap`. The inconsistency is confusing.

**Changes**:

1. After determining the threading model (Step 2.6), make all caches consistent.
2. If single-threaded: all `HashMap`. If multi-threaded: all `ConcurrentHashMap`.

**Verification**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass).

---

### Step 3.6 — Memory: AST Source Retention and Cache Bounds

**Problem**: `JavaSyntaxNode` retains the full source `CharSequence`. Caches grow monotonically.

**Changes** (lower priority, may be deferred):

1. Consider whether source text can be released after all needed information is extracted (e.g., after all lazy properties are initialized).
2. Consider bounded caches (LRU) for `classCache`, `supertypeCache`, `inheritedInnerClassesCache` for very large projects.
3. Document expected memory footprint for typical project sizes.

**Verification**: Memory profiling on representative projects.

---

## Execution Order Summary

| Step | Description | Risk | Scope |
|------|-------------|------|-------|
| 1.1 | Replace string comparisons with constants | Low | All files |
| 1.2 | Split `JavaResolutionContext` | Medium | JavaResolutionContext.kt → 3-4 files |
| 1.3 | Extract shared literal parsing | Low | JavaAnnotationOverAst.kt, ConstantEvaluator.kt → JavaLiteralParser.kt |
| 1.4 | Investigate ConstantEvaluator vs FIR | None (investigation) | Documentation |
| 1.5 | File I/O via external service | Medium | JavaClassFinderOverAstImpl.kt, JavaDirectComponentRegistrar.kt |
| 1.6 | Split `JavaClassFinderOverAstImpl` | Medium | JavaClassFinderOverAstImpl.kt → 3-4 files |
| 1.7 | Fix architectural code smells | Low | Multiple files |
| 2.1 | Optimize child lookups | Low | utils.kt, model classes |
| 2.2 | Optimize import extraction | Low | JavaResolutionContext.kt / JavaImportResolver.kt |
| 2.3 | Optimize inherited inner class traversal | Low | JavaClassFinderOverAstImpl.kt / JavaSupertypeGraph.kt |
| 2.4 | Add `textEquals` method | Low | utils.kt |
| 2.5 | Optimize negative lookups | Low | CombinedJavaClassFinder.kt |
| 2.6 | Evaluate HashMap vs ConcurrentHashMap | Low | Multiple files |
| 2.7 | Richer lightweight index | Medium | JavaSourceIndex.kt |
| 3.1 | Improve extractImports readability | Low | JavaImportResolver.kt |
| 3.2 | Document and test fragile invariants | Low | Test files |
| 3.3 | Address remaining TODOs | Low-Medium | Multiple files |
| 3.4 | Improve isAnnotationType robustness | Low | JavaClassOverAst.kt |
| 3.5 | Consistent cache types | Low | Multiple files |
| 3.6 | Memory: AST retention and cache bounds | Low | Multiple files |

**After each step**: Run all module tests via `./gradlew :kotlin-java-direct:test` (2744+ tests must pass) to verify no regressions.
