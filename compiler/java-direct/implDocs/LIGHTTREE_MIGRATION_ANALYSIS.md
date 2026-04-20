# LightTree Migration Analysis for java-direct

**Date**: 2026-04-20  
**Context**: java-direct's materialized AST is the largest single architectural contributor to its ~20% performance gap versus the PSI-based approach. This document analyzes how Kotlin's LightTree representation works, how it compares to java-direct's current `JavaSyntaxNode`, and proposes a migration path.

---

## Part 1: How Kotlin's LightTree Works

### 1.1 Architecture Overview

The LightTree is an alternative to full PSI trees for representing parsed source code. It was designed for the K2/FIR compiler frontend to avoid the memory overhead of materializing every syntax node as a Java object.

**Key principle**: Nodes are not individual objects in a tree. Instead, parsing produces a *flat production marker list* — an array of (start, end, type) triples. A `FlyweightCapableTreeStructure<LighterASTNode>` adapter provides a tree-like API over this flat data, creating `LighterASTNode` references only on demand.

### 1.2 Parsing Pipeline

```
Source code (CharSequence)
    │
    ▼
Lexer → Token stream (flat arrays: types[], starts[], ends[])
    │
    ▼
Parser → Production markers (start/done pairs in flat IntArrayList)
    │
    ▼
PsiBuilder.lightTree → FlyweightCapableTreeStructure<LighterASTNode>
```

**Entry point** (`compiler/psi/parser/src/.../KotlinLightParser.kt:20-42`):
```kotlin
fun buildLightTree(code: CharSequence, ...): FlyweightCapableTreeStructure<LighterASTNode> {
    val builder = PsiBuilderFactory.getInstance()
        .createBuilder(KotlinParserDefinition(), KotlinLexer(), code)
    // Parser writes marker pairs into builder's internal arrays
    KotlinParsing.createForTopLevelNonLazy(builder).parseFile()
    return builder.lightTree  // Returns adapter over flat arrays
}
```

### 1.3 Internal Data Structures

`PsiBuilderImpl` (IntelliJ Platform) stores parsing results in flat arrays:

| Array | Content | Per-entry cost |
|-------|---------|---------------|
| `myLexTypes[]` | Token element types | 4 bytes (reference, compressed oops) |
| `myLexStarts[]` | Token start offsets | 4 bytes (int) |
| Production markers | Start/done pairs with type + token index range | ~24 bytes per composite node |

**Composite nodes** are represented as marker pairs:
- **Start marker**: records start-token-index, element type
- **Done marker**: records end-token-index, same element type  
- Nesting is implicit from the marker order (parent's start comes before child's start, parent's done comes after child's done)

**Tokens (leaves)** are not stored as explicit markers — they exist implicitly as gaps between composite markers, accessed by index into the lexer output arrays.

### 1.4 FlyweightCapableTreeStructure Interface

```kotlin
interface FlyweightCapableTreeStructure<T> {
    fun getRoot(): T
    fun getParent(node: T): T?
    fun getChildren(node: T, nodesRef: Ref<Array<T?>>): Int  // Fills array, returns count
    fun getStartOffset(node: T): Int
    fun getEndOffset(node: T): Int
    fun toString(node: T): CharSequence
    fun disposeChildren(nodes: Array<out T>?, count: Int)
}
```

**How `getChildren()` works**: For a composite marker with start-token-index `s` and end-token-index `e`, children are all markers and tokens whose ranges fall within `[s, e)`. The implementation scans the flat marker list and lexer arrays to produce child `LighterASTNode` references into a pre-allocated `Ref<Array<>>`.

**LighterASTNode** itself is lightweight — it's either:
- A reference to a composite marker (index into the marker array)
- A reference to a token (index into the lexer arrays)

Both expose: `tokenType: IElementType`, `startOffset: Int`, `endOffset: Int`.

### 1.5 How FIR Consumes LightTree

The converter (`compiler/fir/raw-fir/light-tree2fir/src/.../AbstractLightTreeRawFirBuilder.kt:25-186`) provides traversal utilities:

```kotlin
abstract class AbstractLightTreeRawFirBuilder(
    val tree: FlyweightCapableTreeStructure<LighterASTNode>, ...
) {
    // Get all children as an array (on-demand allocation)
    fun LighterASTNode?.getChildrenAsArray(): Array<out LighterASTNode?> {
        val kidsRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this!!, kidsRef)
        return kidsRef.get()
    }

    // Iterate children, skipping whitespace/comments/errors
    protected inline fun LighterASTNode.forEachChildren(f: (LighterASTNode) -> Unit) {
        val kidsArray = this.getChildrenAsArray()
        for (kid in kidsArray) {
            if (kid == null) break
            if (ignoredTokens.contains(kid.tokenType)) continue
            f(kid)
        }
    }

    // Find child by type
    override fun LighterASTNode.getChildNodeByType(type: IElementType): LighterASTNode? {
        return getChildrenAsArray().firstOrNull { it?.tokenType == type }
    }
}
```

**Conversion pattern**: The builder traverses the LightTree top-down, switching on `child.tokenType` to dispatch to type-specific converters (`convertClass()`, `convertFunctionDeclaration()`, etc.). Each converter extracts the data it needs from children and produces FIR nodes. The tree is traversed once and then becomes eligible for GC (though it's retained indirectly via `KtLightSourceElement` references in FIR nodes).

### 1.6 Memory Characteristics

| Metric | LightTree | Full materialized tree (java-direct) |
|--------|-----------|--------------------------------------|
| **Per-composite-node** | ~24 bytes (marker pair in flat array) | ~56 bytes (JavaSyntaxNode object) + ArrayList overhead |
| **Per-token** | 0 bytes (implicit in lexer arrays) | ~56 bytes (JavaSyntaxNode with emptyList children) |
| **Children storage** | 0 bytes per node (computed from ranges) | ArrayList per node: 48-72 bytes |
| **Parent reference** | Computed on demand via `getParent()` | 8-byte `parent` reference stored on each node |
| **Text access** | Substring of source CharSequence | Cached `@Volatile` String per node |
| **1000-line file (~15K nodes)** | ~100-200 KB (lexer arrays + markers) | ~1.5 MB (node objects + lists + overhead) |
| **500 files** | ~50-100 MB | ~750 MB |

The 7-10x memory difference comes from:
1. **Token nodes are free** in LightTree (majority of nodes are tokens)
2. **No per-node object allocation** — nodes are array indices, not heap objects
3. **No children lists** — parent-child is encoded positionally
4. **No parent references** — computed from nesting order

---

## Part 2: java-direct's Current AST

### 2.1 JavaSyntaxNode Structure

Defined in `compiler/java-direct/src/.../utils.kt:15-98`:

```kotlin
class JavaSyntaxNode(
    val type: SyntaxElementType,           // 8 bytes
    val children: List<JavaSyntaxNode>,    // 8 bytes (reference to ArrayList or emptyList)
    val source: CharSequence,              // 8 bytes (shared)
    val startOffset: Int,                  // 4 bytes
    val endOffset: Int,                    // 4 bytes
    var parent: JavaSyntaxNode? = null,    // 8 bytes
) {
    @Volatile private var cachedText: String? = null           // 8 bytes
    @Volatile private var cachedChildByTypeIndex: Map<...>? = null  // 8 bytes
    // Object header: ~16 bytes
    // Total: ~72 bytes per node (conservative)
}
```

### 2.2 How the Tree Is Built

`buildSyntaxTree()` (utils.kt:100-155) processes the KMP parser's production markers:

```kotlin
fun buildSyntaxTree(builder: SyntaxTreeBuilder, source: CharSequence): JavaSyntaxNode {
    val productionMarkers = prepareProduction(builder).productionMarkers
    val tokens = builder.tokens
    // Stack-based construction: materializes EVERY token and composite as a JavaSyntaxNode
    // Sets parent references, builds children lists
}
```

**Every token becomes a `JavaSyntaxNode`** — keywords, identifiers, brackets, operators, semicolons. For a typical method with 50 tokens + 15 composites = 65 nodes = ~4.7 KB. A file with 20 methods: ~1300 nodes = ~94 KB just for the AST.

### 2.3 How Model Classes Consume the AST

Model classes (`JavaClassOverAst`, `JavaMemberOverAst`, `JavaTypeOverAst`) access the AST through these patterns:

| Pattern | Usage count | Example |
|---------|-------------|---------|
| `node.children.find { it.type == X }` | ~30 | Finding identifier tokens, modifier lists |
| `node.findChildByType(type)` | ~40 | Finding TYPE, MODIFIER_LIST, EXTENDS_LIST |
| `node.getChildrenByType(type)` | ~20 | Getting all METHOD children, ANNOTATION children |
| `node.children.filter { ... }` | ~12 | Filtering by complex predicates |
| `node.children.any { ... }` | ~8 | Checking for modifier keywords |
| `node.text` / `node.textEquals(...)` | ~15 | Reading identifier/keyword text |
| `node.children` (direct iteration) | ~52 | Custom traversal patterns |
| `node.type` | ~93 | Type checking in all patterns |
| `node.parent` | ~5 | Navigating up (field → sibling fields) |

**Total**: ~93 direct references to `node.*` properties across 11 files.

### 2.4 Retention Pattern

AST trees are retained indefinitely via `JavaClassFinderOverAstImpl.classCache`:
- `classCache: ConcurrentHashMap<ClassId, JavaClass>` holds `JavaClassOverAst` instances
- Each `JavaClassOverAst` holds `node: JavaSyntaxNode` → entire file's AST
- AST lives for the entire compilation session
- Lazy properties on model classes dereference `node` on demand, so the AST cannot be released early

---

## Part 3: The Critical Insight — SyntaxTreeBuilder Already Has Production Markers

The KMP parser's `SyntaxTreeBuilder` (used by java-direct) produces **the same flat production marker structure** that PsiBuilder uses internally to create LightTree. The key API:

```kotlin
val result = prepareProduction(builder)  // Finalize parsing
result.productionMarkers  // ProductionMarkerList — flat list of start/done/error markers
builder.tokens            // TokenList — flat arrays of token types, starts, ends
```

**This is exactly the data that a `FlyweightCapableTreeStructure` wraps.** Currently, `buildSyntaxTree()` materializes this flat data into a tree of `JavaSyntaxNode` objects. The optimization is to *skip that materialization step entirely* and create a `FlyweightCapableTreeStructure` adapter over the production markers instead.

---

## Part 4: Proposed Migration Approach

### 4.1 Strategy: Custom FlyweightCapableTreeStructure Over SyntaxTreeBuilder Output

Create a `JavaLightTree` class that implements `FlyweightCapableTreeStructure<JavaLightNode>` directly over the `ProductionMarkerList` and `TokenList` from `SyntaxTreeBuilder`.

```
SyntaxTreeBuilder
    │
    ├── productionMarkers: ProductionMarkerList  ─┐
    │                                              │
    └── tokens: TokenList  ───────────────────────┤
                                                   │
                                         JavaLightTree (new)
                                         implements FlyweightCapableTreeStructure<JavaLightNode>
```

### 4.2 New Classes

#### `JavaLightNode`

A lightweight node reference — not a tree node with children, but a cursor into the flat arrays:

```kotlin
// Option A: Value class (zero-allocation when possible)
@JvmInline
value class JavaLightNode(val index: Int)  // index into productionMarkers or token array

// Option B: Sealed class for type safety
sealed class JavaLightNode {
    class Composite(val markerIndex: Int) : JavaLightNode()  // index into production markers
    class Token(val tokenIndex: Int) : JavaLightNode()       // index into token list
}
```

Option A is more memory-efficient (no object allocation when used inline). Option B is clearer but allocates on every `getChildren()` call.

**Recommendation**: Use Option B initially for correctness, profile, then consider Option A if allocation is a bottleneck.

#### `JavaLightTree`

```kotlin
class JavaLightTree(
    private val productionMarkers: ProductionMarkerList,
    private val tokens: TokenList,
    private val source: CharSequence,
) : FlyweightCapableTreeStructure<JavaLightNode> {

    override fun getRoot(): JavaLightNode = Composite(productionMarkers.size - 1)  // Last done marker = root

    override fun getChildren(node: JavaLightNode, ref: Ref<Array<JavaLightNode?>>): Int {
        // For composite: scan production markers + tokens in range to find direct children
        // For token: return 0 (leaf)
    }

    override fun getParent(node: JavaLightNode): JavaLightNode? {
        // Walk production markers to find enclosing composite
    }

    override fun getStartOffset(node: JavaLightNode): Int = when (node) {
        is Composite -> productionMarkers.getMarker(node.markerIndex).getStartOffset()
        is Token -> tokens.getTokenStart(node.tokenIndex)
    }

    override fun getEndOffset(node: JavaLightNode): Int = when (node) {
        is Composite -> productionMarkers.getMarker(node.markerIndex).getEndOffset()
        is Token -> tokens.getTokenEnd(node.tokenIndex)
    }

    override fun toString(node: JavaLightNode): CharSequence =
        source.subSequence(getStartOffset(node), getEndOffset(node))

    // Type access (not in standard interface, but needed by consumers)
    fun getType(node: JavaLightNode): SyntaxElementType = when (node) {
        is Composite -> productionMarkers.getMarker(node.markerIndex).getNodeType()
        is Token -> tokens.getTokenType(node.tokenIndex)!!
    }

    fun textEquals(node: JavaLightNode, expected: String): Boolean {
        val start = getStartOffset(node)
        val length = getEndOffset(node) - start
        if (length != expected.length) return false
        for (i in 0 until length) {
            if (source[start + i] != expected[i]) return false
        }
        return true
    }
}
```

#### `getChildren()` Implementation Detail

The main complexity is in `getChildren()`. The production markers are a flat list where start/done pairs nest implicitly. To find direct children of a composite marker at index `i`:

1. Find the corresponding start marker (production markers store start and done separately)
2. Scan forward from the start marker to the done marker
3. Collect all done markers whose nesting depth is exactly one level below the parent
4. Collect all tokens in gaps between child composites

This is O(k) where k = number of markers + tokens in the range, same as building a children list. The difference is that it's computed **on demand** rather than materialized for all nodes upfront.

**Optimization**: Pre-compute a `parentIndex[]` array during construction (one int per marker, ~4 bytes) to make `getChildren()` and `getParent()` O(1) via index lookups. This is what PsiBuilderImpl does internally.

### 4.3 Alternative: Reuse PsiBuilderImpl's LightTree Directly

Instead of building a custom `FlyweightCapableTreeStructure`, we could:

1. Use IntelliJ's `PsiBuilderImpl` instead of `SyntaxTreeBuilder` for Java parsing
2. Call `builder.lightTree` to get a `FlyweightCapableTreeStructure<LighterASTNode>` for free

**Pros**: Zero new tree infrastructure code; proven, optimized implementation.  
**Cons**: 
- Requires the IntelliJ `PsiBuilder` API, which needs a `ParserDefinition` and `Lexer` from the IntelliJ platform
- Conflicts with the strategic decision (DM 20-11-25) to use the KMP parser to reduce IJ platform coupling
- `PsiBuilderImpl` is in `compileOnly(intellijCore())` — already a dependency, but strengthening it is architecturally undesirable
- Would need to bridge between `IElementType` (IntelliJ) and `SyntaxElementType` (KMP) type systems

**Recommendation**: Build custom `JavaLightTree` over SyntaxTreeBuilder output. This maintains the KMP parser independence while achieving the same memory benefits.

### 4.4 Alternative: Intermediate "Extracted Data" Approach

Instead of keeping the AST (light or materialized) alive for lazy property access, eagerly extract all needed data during construction and store it as plain fields. Model classes would hold extracted data, not AST references.

```kotlin
class JavaClassExtractedData(
    val name: String,
    val modifiers: Set<SyntaxElementType>,
    val typeParameterNodes: List<JavaSyntaxNode>,  // or extracted data
    val supertypeRefs: List<...>,
    val innerClassNodes: List<...>,
    val methodNodes: List<...>,
    // ...
)
```

**Pros**: AST can be GC'd immediately after extraction. Zero ongoing memory.  
**Cons**: Large upfront refactor of all model classes. Risk of missing properties. Hard to maintain.

**Recommendation**: This is the ideal long-term architecture but is a significantly larger effort. The LightTree approach is a better intermediate step.

---

## Part 5: Migration Impact Analysis

### 5.1 Files Requiring Changes

| File | Lines | Change type | Effort |
|------|-------|-------------|--------|
| `utils.kt` | 211 | **Major rewrite**: Replace `JavaSyntaxNode` and `buildSyntaxTree()` with `JavaLightTree` and `JavaLightNode`. Keep `JavaSyntaxNode` temporarily for backward compat. | High |
| `parse.kt` | 39 | **Minor**: Change return type from `SyntaxTreeBuilder` to `JavaLightTree`. | Low |
| `JavaElementOverAst.kt` | 19 | **Moderate**: Change `node: JavaSyntaxNode` to `node: JavaLightNode` + `tree: JavaLightTree`. | Medium |
| `JavaClassOverAst.kt` | 468 | **Major**: Rewrite all `node.findChildByType()` / `node.children` to `tree.getChildren()` pattern. 20+ access sites. | High |
| `JavaMemberOverAst.kt` | 511 | **Major**: Same pattern change. 28+ access sites. | High |
| `JavaTypeOverAst.kt` | 772 | **Major**: Most complex model class. 19+ node access sites, custom traversal in `collectIdentifiers()`, `collectAllRefParamLists()`. | High |
| `JavaAnnotationOverAst.kt` | 375 | **Moderate**: 8+ node access sites. | Medium |
| `ConstantEvaluator.kt` | 293 | **Moderate**: 8+ node access sites. | Medium |
| `JavaImportResolver.kt` | 264 | **Moderate**: 12 JavaSyntaxNode references, 8 `.children` accesses. | Medium |
| `JavaClassFinderOverAstImpl.kt` | 385 | **Moderate**: Change cache from holding AST-referencing classes to LightTree-referencing. | Medium |
| `JavaResolutionContext.kt` | 622 | **Low-Moderate**: 2 JavaSyntaxNode references. Mostly operates on resolved names, not AST. | Low |
| `JavaRecordComponentOverAst.kt` | 33 | **Low**: 4 node accesses. | Low |
| `JavaPackageOverAst.kt` | 33 | **None**: Doesn't use AST. | None |
| `JavaScopeResolver.kt` | 140 | **None**: Operates on JavaClass, not AST. | None |
| `JavaSourceIndex.kt` | 115 | **None**: Operates on file entries. | None |
| `JavaSupertypeGraph.kt` | 246 | **Low**: 1 node access via `.children`. | Low |
| Tests | ~2000 | **Moderate**: Unit tests (`JavaParsingTest`) that verify AST structure would need updating. | Medium |

### 5.2 Quantified Change Estimate

| Category | Estimated lines changed | Files |
|----------|------------------------|-------|
| New infrastructure (`JavaLightTree`, `JavaLightNode`, tree navigation utilities) | ~300-400 new | 1-2 new files |
| Rewritten `buildSyntaxTree()` → light tree construction | ~80 changed | utils.kt |
| Model class adaptations (all `*OverAst` classes) | ~400-500 changed | 8 files |
| Test adaptations | ~100-150 changed | test files |
| **Total** | **~900-1100 lines** | **~12 files** |

### 5.3 Navigation API Adapter

To minimize changes in model classes, introduce a thin navigation layer that mirrors the current `JavaSyntaxNode` API but operates on `JavaLightTree`:

```kotlin
// Extension functions on JavaLightTree that mirror JavaSyntaxNode extensions
fun JavaLightTree.findChildByType(node: JavaLightNode, type: SyntaxElementType): JavaLightNode?
fun JavaLightTree.getChildrenByType(node: JavaLightNode, type: SyntaxElementType): List<JavaLightNode>
fun JavaLightTree.text(node: JavaLightNode): String
fun JavaLightTree.textEquals(node: JavaLightNode, expected: String): Boolean
```

With this adapter, model class changes become mostly mechanical: replace `node.findChildByType(X)` with `tree.findChildByType(node, X)`. The `tree` reference would be passed alongside `node` in constructors.

### 5.4 Phased Migration Plan

**Phase 1: Infrastructure (2-3 days)**
- Implement `JavaLightNode`, `JavaLightTree` with navigation extensions
- Keep `JavaSyntaxNode` and `buildSyntaxTree()` in parallel
- Add unit tests verifying `JavaLightTree` produces identical traversal results

**Phase 2: Parallel Verification (1-2 days)**
- Run both representations in parallel (build materialized tree AND light tree)
- Assert that all model class property evaluations produce identical results
- This catches any traversal bugs before committing to the new representation

**Phase 3: Model Class Migration (3-5 days)**
- Migrate `JavaElementOverAst` base class to hold `JavaLightNode` + `JavaLightTree`
- Migrate model classes one by one: `JavaClassOverAst` → `JavaMemberOverAst` → `JavaTypeOverAst` → rest
- After each class, run full test suite to verify no regressions

**Phase 4: Cleanup (1 day)**
- Remove `JavaSyntaxNode` class and `buildSyntaxTree()` function
- Remove parallel-verification code
- Update documentation

**Total estimate: 7-11 days**

### 5.5 Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| `getChildren()` performance — computed on demand vs. pre-built list | Medium | Pre-compute parent-index array; cache children lists for hot nodes (class bodies) |
| `parent` navigation — 5 call sites use `node.parent` | Low | `JavaLightTree.getParent()` computes from marker nesting; or pass parent explicitly |
| Custom traversal patterns (e.g., `collectIdentifiers()` in `JavaTypeOverAst`) | Medium | These iterate `node.children` recursively; need equivalent recursive iteration over light tree children |
| Thread safety — `getChildren()` allocates arrays | Low | Same pattern as FIR's `Ref<Array<>>` reuse; thread-local refs if needed |
| `node.children.count { }` / `.any { }` patterns | Low | Translate to `getChildren()` + iteration; or add `hasChildOfType()` utility |

---

## Part 6: Expected Performance Impact

### 6.1 Memory Savings

For a project with 500 Java source files (typical large module):

| Component | Current (materialized) | After (LightTree) | Savings |
|-----------|----------------------|-------------------|---------|
| Token nodes (80% of all nodes) | ~600K × 72 bytes = 43 MB | 0 bytes (implicit) | 43 MB |
| Composite nodes (20%) | ~150K × 72 bytes = 11 MB | ~150K × 24 bytes = 3.6 MB | 7.4 MB |
| Children lists | ~150K × 60 bytes = 9 MB | 0 bytes (computed) | 9 MB |
| Parent references | ~750K × 8 bytes = 6 MB | 0 bytes (computed) | 6 MB |
| Lexer arrays (new) | 0 | ~750K × 8 bytes = 6 MB | -6 MB |
| Parent-index array (new) | 0 | ~150K × 4 bytes = 0.6 MB | -0.6 MB |
| **Total** | **~69 MB** | **~10.2 MB** | **~59 MB (~85% reduction)** |

### 6.2 GC Pressure

- **Current**: 750K+ `JavaSyntaxNode` objects created per 500 files, all long-lived (retained in classCache)
- **After**: ~0 node objects created (markers stored in flat arrays). Light tree references created on demand during property access, immediately eligible for GC
- **Expected GC improvement**: Significant reduction in old-gen heap, fewer full GC pauses

### 6.3 CPU Impact

- **Tree construction**: Faster — skip the `buildSyntaxTree()` materialization step entirely
- **Property access**: Slightly slower on first access (compute children from markers vs. pre-built list), but offset by:
  - Avoided upfront materialization cost
  - Better cache locality (flat arrays vs. scattered heap objects)
  - Most properties accessed only 1-2 times
- **Net CPU impact**: Likely neutral or slightly positive overall

### 6.4 Estimated Contribution to Closing the 20% Gap

From PERFORMANCE_REVIEW.md's breakdown:
- Memory/GC pressure accounts for ~40% of the gap (~8% absolute)
- The materialized tree is the largest contributor to memory pressure
- LightTree migration addresses ~60-70% of the memory category

**Expected improvement: 4-6% of total compilation time**, bringing the gap from ~20% to ~14-16%. Combined with the other quick wins from PERFORMANCE_REVIEW.md (cached splits, annotations caching, basename precomputation), the total gap could narrow to ~10-12%.

---

## Part 7: What NOT to Reuse from Kotlin's LightTree

### 7.1 `FlyweightCapableTreeStructure<LighterASTNode>` from IntelliJ

While tempting, reusing the exact IntelliJ `FlyweightCapableTreeStructure<LighterASTNode>` interface has drawbacks:
- **Type system mismatch**: LighterASTNode uses `IElementType` (IntelliJ); java-direct uses `SyntaxElementType` (KMP). Bridging adds complexity.
- **Unnecessary API surface**: The interface requires `disposeChildren()` which is a no-op in all Kotlin implementations but adds ceremony.
- **Coupling**: Depending on `com.intellij.util.diff.FlyweightCapableTreeStructure` from IntelliJ core strengthens the coupling java-direct is designed to avoid.

### 7.2 What CAN Be Reused

- **The design pattern**: Flat array + on-demand node references. Identical algorithmic approach.
- **Navigation utility patterns**: `forEachChildren`, `getChildNodeByType`, `getChildrenAsArray` from `AbstractLightTreeRawFirBuilder` — adapt these for `JavaLightTree`.
- **`Ref<Array<>>` reuse pattern**: Thread-local or stack-allocated array references to avoid allocation in `getChildren()`.

---

## Part 8: Summary

| Aspect | Recommendation |
|--------|---------------|
| **Approach** | Custom `JavaLightTree` over `SyntaxTreeBuilder` production markers |
| **Reuse from Kotlin** | Design pattern and navigation utilities, not classes |
| **Key new classes** | `JavaLightNode` (sealed), `JavaLightTree` (impl), navigation extensions |
| **Estimated effort** | 7-11 days, ~900-1100 lines changed across 12 files |
| **Memory improvement** | ~85% reduction in AST memory (~59 MB for 500-file project) |
| **Performance improvement** | ~4-6% of total compilation time |
| **Risk level** | Medium — large mechanical refactor but well-understood patterns |
| **Migration strategy** | Phased with parallel verification; model classes migrated one-by-one |

The SyntaxTreeBuilder already produces the flat representation that LightTree needs. The current `buildSyntaxTree()` step wastefully materializes this flat data into heap objects. Removing that materialization is the single highest-leverage optimization available for java-direct's memory profile.

---

## Part 9: Platform Dependency Analysis — Kotlin LightTree and a Shared Path Forward

### 9.1 Kotlin's Current LightTree Platform Dependencies

Kotlin's LightTree infrastructure currently depends on **10+ IntelliJ Platform classes** from `intellij-core`:

| Class | Package | Role |
|-------|---------|------|
| `LighterASTNode` | `com.intellij.lang` | Core node interface |
| `LighterASTTokenNode` | `com.intellij.lang` | Token node subtype |
| `FlyweightCapableTreeStructure<T>` | `com.intellij.util.diff` | Tree structure interface |
| `PsiBuilder` | `com.intellij.lang` | Parser builder interface |
| `PsiBuilderFactory` | `com.intellij.lang` | Builder factory |
| `PsiBuilderImpl` | `com.intellij.lang.impl` | Builder impl (`.lightTree` property, `getErrorMessage()`) |
| `TreeBackedLighterAST` | `com.intellij.lang` | PSI→LightTree bridge |
| `IElementType` | `com.intellij.psi.tree` | Element/token type identifier |
| `TokenSet` | `com.intellij.psi.tree` | Immutable set of types |
| `TokenType` | `com.intellij.psi` | Special token constants (ERROR_ELEMENT, etc.) |
| `Ref<T>` | `com.intellij.openapi.util` | Mutable output-parameter holder |

These dependencies permeate deeply:

- **207 occurrences** of `FlyweightCapableTreeStructure` across 23 files in `compiler/`
- **99 occurrences** of `KtLightSourceElement` across 34 files
- Key spread: `KtSourceElement.kt` (core), all FIR raw-fir builders, all FIR checkers/syntax checkers, all diagnostic positioning strategies

The dependency chain is: `KotlinLightParser` → `PsiBuilderFactory` → `PsiBuilderImpl` → `FlyweightCapableTreeStructure<LighterASTNode>` → throughout FIR and diagnostics via `KtLightSourceElement.treeStructure`.

### 9.2 Can These Platform Parts Be Extracted?

**The core interfaces are small.** The API surface that matters is:

```
LighterASTNode          — ~10 lines (interface: tokenType, startOffset, endOffset)
FlyweightCapableTreeStructure<T> — ~10 lines (interface: 7 methods)
Ref<T>                  — ~5 lines (trivial mutable holder)
IElementType            — ~30 lines (class with debug name + index)
TokenSet                — ~50 lines (bitset over IElementType indices)
```

**Total core API: ~100-120 lines of interface/class definitions.** These are individually trivial.

**But the implementation is not.** `PsiBuilderImpl` is the heavyweight — it's ~3000+ lines of complex parsing infrastructure that builds the flat marker representation and produces the `FlyweightCapableTreeStructure`. Extracting it means pulling `PsiBuilderImpl` + `ASTFactory` + marker pool management + the concrete `FCTSImpl` (the internal class that implements `FlyweightCapableTreeStructure`).

**Realistic extraction scope:**
- Interfaces (`LighterASTNode`, `FlyweightCapableTreeStructure`, `Ref`): ~150 lines — trivially extractable
- Type system (`IElementType`, `TokenSet`): ~200 lines — extractable but `IElementType` uses a global index counter, needs care
- Builder impl (`PsiBuilderImpl`): ~3000 lines — complex, with internal IntelliJ dependencies (logging, diagnostics, pool management)
- **Bridge code** (`TreeBackedLighterAST`): Not needed if PSI path is abandoned

**Bottom line**: Extracting *just the interfaces* is cheap. Extracting the *builder implementation* that produces the tree is expensive and fragile — it's tightly coupled to IntelliJ internals.

### 9.3 The KMP Parser Already Solves This Problem

The KMP (Kotlin Multiplatform Parser) infrastructure provides a **completely platform-free alternative** that renders extraction unnecessary:

**KMP Kotlin parser** (`compiler/multiplatform-parsing/`):
- Status: Experimental (`@ApiStatus.Experimental`), used only in tests
- Uses `SyntaxTreeBuilder` from `com.intellij.platform.syntax` (platform-neutral, published separately)
- Produces `ProductionMarkerList` + `TokenList` — the same flat representation as PsiBuilder's internals
- **Zero dependency on IntelliJ Platform**: Only depends on `com.intellij.platform.syntax.*`, which uses `SyntaxElementType` (not `IElementType`)
- Already validated against the old PSI parser via `FullParserTestsWithPsi` and `FullParserTestsWithLightTree`

**KMP Java parser** (used by java-direct):
- Production-ready, already integrated
- Same `SyntaxTreeBuilder` + `ProductionMarkerList` + `TokenList` output
- Same `com.intellij.platform.syntax.*` dependency

**The two type systems**:
| | IntelliJ Platform (old) | KMP Syntax API (new) |
|---|---|---|
| Element types | `IElementType` | `SyntaxElementType` |
| Type sets | `TokenSet` | `SyntaxElementTypeSet` |
| Parser builder | `PsiBuilder` | `SyntaxTreeBuilder` |
| Tree output | `FlyweightCapableTreeStructure<LighterASTNode>` | `ProductionMarkerList` + `TokenList` |
| Token constants | `TokenType.ERROR_ELEMENT` etc. | `SyntaxTokenTypes.ERROR_ELEMENT` etc. |

The KMP Syntax API (`com.intellij.platform.syntax`) is a **clean-room replacement** for the relevant subset of the IntelliJ Platform parser infrastructure. It's multiplatform-capable, separately published, and already used by both the KMP Kotlin parser and java-direct.

### 9.4 Proposed Unified Architecture

The approach described in sections 4.1/4.2 (`JavaLightTree` over `SyntaxTreeBuilder` output) can serve as the pattern for **both** Java and Kotlin, eliminating the IntelliJ Platform dependency for both:

```
                    com.intellij.platform.syntax (KMP, platform-neutral)
                    ┌─────────────────────────────────────────────────┐
                    │  SyntaxTreeBuilder                              │
                    │  SyntaxElementType                              │
                    │  ProductionMarkerList + TokenList               │
                    └──────────────┬──────────────────────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
             Java KMP Parser              Kotlin KMP Parser
          (java-direct, production)      (multiplatform-parsing, experimental)
                    │                             │
                    ▼                             ▼
              JavaLightTree                KotlinLightTree  (NEW)
          (custom, proposed in 4.1)      (same pattern as JavaLightTree)
                    │                             │
                    ▼                             ▼
           java-direct model            FIR raw-fir builder
           classes (*OverAst)           (LightTreeRawFirDeclarationBuilder etc.)
```

**What `KotlinLightTree` would look like:**
- Same design as `JavaLightTree`: adapter over `ProductionMarkerList` + `TokenList`
- Same `SyntaxElementType`-based type system (no `IElementType` bridge)
- Would replace `KotlinLightParser.buildLightTree()` → `PsiBuilder.lightTree`
- The `LightTree2Fir` converter would use `KotlinLightTree` instead of `FlyweightCapableTreeStructure<LighterASTNode>`

### 9.5 Migration Path for Kotlin (Replacing Platform LightTree)

**Phase A: Create shared LightTree infrastructure** (~1-2 weeks)

Extract the common pattern into a shared module (e.g., `compiler/syntax-tree/` or part of the syntax-api library):

```kotlin
// Shared interface — platform-free replacement for FlyweightCapableTreeStructure
interface SyntaxLightTree {
    fun getRoot(): SyntaxLightNode
    fun getParent(node: SyntaxLightNode): SyntaxLightNode?
    fun getChildren(node: SyntaxLightNode): SyntaxLightNodeList  // or Array-based
    fun getStartOffset(node: SyntaxLightNode): Int
    fun getEndOffset(node: SyntaxLightNode): Int
    fun getType(node: SyntaxLightNode): SyntaxElementType
    fun getText(node: SyntaxLightNode): CharSequence
}

// Concrete implementation over ProductionMarkerList + TokenList
class ProductionBasedLightTree(
    private val markers: ProductionMarkerList,
    private val tokens: TokenList,
    private val source: CharSequence,
) : SyntaxLightTree { ... }
```

Both `JavaLightTree` and `KotlinLightTree` could either be this class directly, or thin language-specific wrappers.

**Phase B: Migrate java-direct** (as described in Part 5, ~7-11 days)

java-direct is the ideal first adopter because:
- Smaller codebase (~5K lines vs ~50K+ for FIR light-tree2fir)
- No `KtSourceElement` dependency (java-direct doesn't produce `KtLightSourceElement`)
- Already uses `SyntaxTreeBuilder`

**Phase C: Migrate Kotlin FIR** (weeks-months, large effort)

This is the bigger undertaking because:

1. **`KtLightSourceElement` permeates FIR**: 99 occurrences across 34 files. Every FIR node stores `source: KtSourceElement?` which, in LightTree mode, is a `KtLightSourceElement` holding a `FlyweightCapableTreeStructure<LighterASTNode>` reference. Replacing this means:
   - New `KtSourceElement` variant backed by `SyntaxLightTree` instead of `FlyweightCapableTreeStructure<LighterASTNode>`
   - Or making `KtLightSourceElement` generic over the tree type

2. **Diagnostic positioning**: `LightTreePositioningStrategies.kt` has 136 occurrences of `FlyweightCapableTreeStructure` — it navigates the tree to produce error positions. All of this needs to work with the new tree type.

3. **FIR raw-fir builders**: `AbstractLightTreeRawFirBuilder` and its subclasses (~2000+ lines) traverse `FlyweightCapableTreeStructure<LighterASTNode>`. These become `SyntaxLightTree` consumers.

4. **KMP Kotlin parser maturity**: The parser is experimental and not yet validated for production. Before Kotlin can switch, the KMP parser must reach parity with the old `KotlinParsing.java`.

**Estimated effort for Phase C**: 4-8 weeks, including making the KMP parser production-ready.

### 9.6 Size of What Needs Extraction vs. What's Already Available

| Component | Lines (approx) | Status |
|-----------|----------------|--------|
| `SyntaxElementType` (replaces `IElementType`) | ~30 | Already in `com.intellij.platform.syntax` |
| `SyntaxElementTypeSet` (replaces `TokenSet`) | ~40 | Already in `com.intellij.platform.syntax` |
| `SyntaxTreeBuilder` (replaces `PsiBuilder`) | ~200 interface | Already in `com.intellij.platform.syntax` |
| `ProductionMarkerList` / `TokenList` | ~100 interface | Already in `com.intellij.platform.syntax` |
| `SyntaxTreeBuilderImpl` (replaces `PsiBuilderImpl`) | ~1500 | Already in `com.intellij.platform.syntax.impl` |
| **LightTree adapter** (replaces `FlyweightCapableTreeStructure`) | ~200-300 | **Needs to be written** (this is the `JavaLightTree`/`KotlinLightTree` class) |
| `Ref<T>` equivalent | ~5 | Trivial, or just use array parameter |

**Key insight**: The KMP Syntax API (`com.intellij.platform.syntax`) already replaces 90% of what needs extracting. The only missing piece is the **light tree adapter** — the ~200-300 line class that wraps `ProductionMarkerList`+`TokenList` as a navigable tree. That's exactly what `JavaLightTree` (section 4.2) is.

### 9.7 Summary: Two Paths, One Destination

**Path 1 (Extract IntelliJ classes)**: Copy `LighterASTNode`, `FlyweightCapableTreeStructure`, `PsiBuilderImpl` internals into the Kotlin repo. Maintain them independently.
- Pros: Immediate, no API changes in FIR
- Cons: ~3000+ lines of complex code to own; still uses `IElementType`; doesn't help java-direct

**Path 2 (Use KMP Syntax API + custom LightTree adapter)**: Build `ProductionBasedLightTree` over `SyntaxTreeBuilder` output. Migrate consumers from `FlyweightCapableTreeStructure<LighterASTNode>` to the new tree.
- Pros: Clean break from IntelliJ Platform; reuses existing KMP infrastructure; shared between Java and Kotlin; small new code (~300 lines)
- Cons: Large migration in FIR consumers (~207 `FlyweightCapableTreeStructure` occurrences to update)

**Recommendation**: Path 2 is the right long-term direction. The KMP Syntax API already provides the parser-side solution. The only gap is the ~300-line light tree adapter. java-direct should build this first (Phase B), proving the approach, then Kotlin FIR can follow (Phase C) when the KMP Kotlin parser matures to production readiness.

---

## Part 10: The Analysis API Constraint and Deeper Abstraction

### 10.1 The Three-World Problem

The Kotlin compiler infrastructure serves three distinct runtime contexts, each with different source representation requirements:

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. IntelliJ IDE (Analysis API)                                   │
│    PSI is mandatory — the entire IDE ecosystem operates on PSI.  │
│    Recoding is not feasible.                                     │
│    Path: KtFile (PSI) → psi2fir → FIR → Analysis API (Ka*)      │
├──────────────────────────────────────────────────────────────────┤
│ 2. CLI Compiler (current LightTree)                              │
│    PSI-free for Kotlin sources via LightTree.                    │
│    Path: CharSequence → PsiBuilder.lightTree → FIR → IR → JVM   │
│    Still depends on IntelliJ Platform (PsiBuilder, IElementType) │
├──────────────────────────────────────────────────────────────────┤
│ 3. Platform-Free Compiler (aspirational)                         │
│    No IntelliJ dependency at all.                                │
│    Path: CharSequence → KMP Parser → ProductionBasedLightTree    │
│          → FIR → IR → backends                                   │
│    Also: Analysis API without IntelliJ (standalone, LSP, etc.)   │
└──────────────────────────────────────────────────────────────────┘
```

The challenge: **FIR must serve all three worlds** through a single code path. And currently, world 1 (PSI) and world 2 (LightTree) each leak their specifics into FIR.

### 10.2 Current State of the Abstraction

**Where it works well:**

The raw-fir builder uses a generic type parameter `T` to abstract over input:

```kotlin
// compiler/fir/raw-fir/raw-fir.common/
abstract class AbstractRawFirBuilder<T : Any>(session: FirSession, context: Context<T>) {
    abstract fun T.toFirSourceElement(kind: KtFakeSourceElementKind?): KtSourceElement
    abstract val T.elementType: IElementType
    abstract val T.asText: String
    // ...
}
```

Two concrete implementations:
- `PsiRawFirBuilder : AbstractRawFirBuilder<PsiElement>` (psi2fir)
- `AbstractLightTreeRawFirBuilder : AbstractRawFirBuilder<LighterASTNode>` (light-tree2fir)

FIR elements themselves store `source: KtSourceElement?` (abstract type) — correct.

**Where it leaks:**

`KtSourceElement` (the abstract base) declares **LightTree-specific** properties as part of its abstract interface:

```kotlin
sealed class KtSourceElement : AbstractKtSourceElement() {
    abstract val lighterASTNode: LighterASTNode           // ← forces PSI to wrap as LightTree
    abstract val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>  // ← same
    abstract val elementType: IElementType?
}
```

This forces `KtPsiSourceElement` to lazily construct `TreeBackedLighterAST` wrappers so it can satisfy the abstract contract — wasted work and a violation of separation.

**Measured leak sites across the codebase:**

| Leak pattern | Count | Where |
|---|---|---|
| `source?.psi` (PSI access from FIR) | ~41 | FIR checkers (21), low-level-api-fir (20) |
| `source?.lighterASTNode` / `.treeStructure` | ~35 | FIR checkers (26), specialized code (9) |
| `as KtPsiSourceElement` (explicit cast) | ~7 | PsiRawFirBuilder, diagnostics, generated code |
| `as? KtPsiSourceElement` (safe cast) | ~2 | Utils.kt (.psi extension), incremental |
| `when (source) { is KtPsi... / is KtLight... }` | ~3 | Syntax checkers (the "correct" pattern) |
| **Total abstraction violations** | **~88** | Across ~30 files |

### 10.3 Analysis API: PSI Is Foundational, Not Just a Detail

The Analysis API (`analysis/`) is deeply coupled to PSI at the **API contract level**, not just implementation:

1. **Entry point requires PSI**: `analyze(useSiteElement: KtElement, action: KaSession.() -> R)` — the starting point of every analysis is a PSI node.

2. **Symbols expose PSI**: `KaSymbol.psi: PsiElement?` is part of the public API contract. Every symbol can return its backing PSI element.

3. **Platform interface returns PSI**: `KotlinDeclarationProvider` methods return `KtClassLikeDeclaration`, `KtProperty`, `KtNamedFunction` — all PSI types. Any platform implementation must provide declarations as PSI.

4. **FIR-to-PSI mapping is the core mechanism**: `low-level-api-fir` maps `KtElement` → `FirElement` using the `source` property. The low-level API has 20 `source?.psi` accesses — this is the **primary navigation mechanism**, not a leak.

5. **Standalone mode still uses PSI**: `analysis-api-standalone/` creates `CoreApplicationEnvironment` and full PSI trees. There is no PSI-free standalone mode.

**This is by design, not an accident.** In the IDE, PSI is the canonical representation of source code. The user navigates, edits, and inspects PSI elements. Analysis API provides semantic information *about* PSI elements. Removing PSI from this equation would require replacing the entire IDE editing/navigation stack.

### 10.4 What a Clean Abstraction Would Look Like

The goal is not to remove PSI from the IDE path, but to make FIR (and potentially Analysis API) agnostic about **which** parsed representation provided the source:

```
                    ┌──────────────────────────────┐
                    │     AbstractSourceElement     │  ← New, minimal
                    │  startOffset: Int             │
                    │  endOffset: Int               │
                    │  elementType: SyntaxElementType │  ← KMP type, not IElementType
                    └──────────┬───────────────────┘
                               │
              ┌────────────────┼────────────────────┐
              │                │                    │
    KtPsiSourceElement   KtLightSourceElement   KtSyntaxSourceElement (NEW)
    (wraps PsiElement)   (wraps LighterASTNode) (wraps SyntaxLightNode)
    IDE path             Old CLI path            New platform-free path
```

**Key changes from current design:**

1. **Remove `lighterASTNode` and `treeStructure` from the abstract base.** These are LightTree-specific and should only exist on `KtLightSourceElement`.

2. **Replace `IElementType` with `SyntaxElementType` in the abstract interface.** The KMP type system is the portable one. PSI can bridge via `IElementType.toSyntaxElementType()` (mapping is trivial — same debug names, same semantics).

3. **Add `KtSyntaxSourceElement`** that wraps a `SyntaxLightNode` + `SyntaxLightTree` reference (from the KMP path). This is what java-direct and eventually the KMP Kotlin parser would produce.

4. **Make `SourceNavigator` the mandatory abstraction for tree navigation from FIR.** Currently ~88 call sites bypass it. These should route through `SourceNavigator` (or a similar dispatch interface) instead of directly accessing `.psi`, `.lighterASTNode`, or `.treeStructure`.

### 10.5 FIR Checker Abstraction Strategy

FIR checkers are the biggest leak area (26 LightTree accesses, 21 PSI accesses). They need to inspect the **syntax** of source code to produce diagnostics (e.g., "redundant modifier", "confusing when branch").

Currently, many checkers have dual implementations:

```kotlin
// Current: explicit dual-path in each checker
class FirSomeSyntaxChecker : FirSyntaxChecker<KtElement, P>() {
    override fun checkPsi(element: FirElement, source: KtPsiSourceElement, psi: P) { ... }
    override fun checkLightTree(element: FirElement, source: KtLightSourceElement) { ... }
}
```

**This already has the right shape** — the checker framework dispatches to PSI or LightTree paths. Adding a third path for `KtSyntaxSourceElement` would mean adding a third method:

```kotlin
override fun checkSyntaxTree(element: FirElement, source: KtSyntaxSourceElement) { ... }
```

But the LightTree and SyntaxTree paths would likely share most logic (both navigate a tree by element types). The real duality is **PSI vs. non-PSI**, not three-way.

A cleaner design:

```kotlin
abstract class FirSyntaxChecker {
    // Single method for all non-PSI paths
    open fun checkTree(element: FirElement, tree: SyntaxTreeNavigator) { ... }
    // PSI override (optional, for IDE-specific optimizations)
    open fun checkPsi(element: FirElement, psi: PsiElement) {
        // Default: wrap PSI as SyntaxTreeNavigator and delegate to checkTree()
        checkTree(element, PsiSyntaxTreeNavigator(psi))
    }
}
```

Where `SyntaxTreeNavigator` is a minimal interface for "find child by type, get text, iterate children" — exactly what both LightTree and SyntaxLightTree provide.

### 10.6 Analysis API Without IntelliJ: What It Would Take

For Analysis API to work on top of the KMP parser without IntelliJ (e.g., for LSP servers, build tools, or standalone analysis), the following would need to change:

| Component | Current (PSI-required) | Needed (PSI-optional) | Effort |
|---|---|---|---|
| **Entry point** | `analyze(KtElement)` | `analyze(KaSourcePointer)` where pointer can be PSI or offset-based | Medium |
| **Symbol.psi** | Returns `PsiElement?` | Returns `KaSourceElement?` (abstract) | Medium — API break |
| **Platform interface** | Returns PSI types (`KtClass`, etc.) | Returns abstract declaration descriptors | High — full redesign |
| **Low-level FIR API** | Maps PSI↔FIR via `source?.psi` | Maps abstract source↔FIR via source offsets/IDs | High |
| **FIR source** | `KtSourceElement` with PSI/LightTree leak | Clean `AbstractSourceElement` | Medium |
| **Standalone mode** | Uses `CoreApplicationEnvironment` + PSI | Direct FIR construction from KMP parser | Medium |

**The realistic path is incremental:**

1. **Phase 1 (FIR abstraction)**: Clean up the 88 leak sites. Make `KtSourceElement` truly abstract (remove `lighterASTNode`/`treeStructure` from base). Route all source access through `SourceNavigator`. This benefits everyone — makes FIR code cleaner and enables java-direct/KMP paths. **Effort: 2-4 weeks.**

2. **Phase 2 (Third source type)**: Add `KtSyntaxSourceElement` for the KMP parser path. java-direct produces these. FIR handles them via existing abstraction. **Effort: 1-2 weeks** (after Phase 1).

3. **Phase 3 (Analysis API abstraction)**: This is the big one. Define `KaSourcePointer` (abstract source reference that can be PSI or offset-based). Add `KaSourceElement` wrapper. Modify platform interface to optionally work without PSI. **Effort: months**, and requires a deliberate API evolution strategy since Analysis API has external consumers.

4. **Phase 4 (PSI-free standalone)**: Build a standalone Analysis API session that uses KMP parser + `KtSyntaxSourceElement` instead of PSI. This enables LSP servers and build tools to use Analysis API without IntelliJ. **Effort: months** after Phase 3.

### 10.7 Impact on the java-direct LightTree Migration

The Analysis API constraint **does not block** the java-direct migration described in Parts 4-5. Here's why:

- java-direct produces `JavaClass` / `JavaMethod` / etc. — it does not produce `KtSourceElement` or FIR elements
- FIR consumes java-direct's output through the `JavaClass` interface, which is already abstract
- java-direct's internal AST representation (whether `JavaSyntaxNode` or `JavaLightTree`) is invisible to FIR and Analysis API
- The `JavaClassifierType`, `JavaAnnotation`, etc. interfaces are representation-agnostic

**However**, the java-direct migration is a **proving ground** for the infrastructure that would later serve the KMP Kotlin parser:

```
Phase B (java-direct):  SyntaxTreeBuilder → JavaLightTree → JavaClass interfaces
                         ↑ proves the pattern

Phase C (Kotlin FIR):   SyntaxTreeBuilder → KotlinLightTree → FIR elements
                         ↑ same pattern, but requires KtSourceElement cleanup first
```

### 10.8 Revised Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Analysis API (Ka*)                               │
│  analyze(KtElement) → KaSession → KaSymbol, KaType, ...               │
│  Currently: PSI mandatory. Future: abstract source pointer.            │
├─────────────────────────────────────────────────────────────────────────┤
│                              FIR                                        │
│  FirElement.source: AbstractSourceElement (cleaned up)                 │
│  SourceNavigator dispatches: PSI / LightTree / SyntaxTree              │
├─────────────┬──────────────────┬────────────────────┬──────────────────┤
│  psi2fir    │  light-tree2fir  │  syntax-tree2fir   │  java-fir        │
│  (IDE)      │  (old CLI)       │  (new CLI, future) │  (java sources)  │
│  PsiElement │  LighterASTNode  │  SyntaxLightNode   │  JavaClass iface │
├─────────────┴──────────────────┴────────────────────┴──────────────────┤
│                        Parser Layer                                     │
│  Old: PsiBuilder (IntelliJ)        New: SyntaxTreeBuilder (KMP)        │
│  KotlinParsing.java                KotlinParser.kt (experimental)      │
│                                    JavaParser (production, java-direct) │
├─────────────────────────────────────────────────────────────────────────┤
│                     com.intellij.platform.syntax                        │
│  SyntaxElementType, SyntaxTreeBuilder, ProductionMarkerList, TokenList  │
│  Platform-neutral, multiplatform-capable, separately published          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 10.9 Summary: What Changes, What Doesn't

| Aspect | Changes? | Notes |
|---|---|---|
| **Analysis API public contract** | No (near-term) | PSI stays mandatory for IDE. Abstract source pointer is a future evolution. |
| **Analysis API standalone** | Eventually | Could work PSI-free once FIR abstraction is clean + KMP parser is production-ready |
| **FIR core** | Yes (cleanup) | Remove LightTree/PSI leaks from `KtSourceElement`. Route through `SourceNavigator`. |
| **FIR checkers** | Yes (cleanup) | ~88 leak sites need to use abstraction instead of direct `source?.psi` / `.treeStructure` |
| **raw-fir builders** | No (already good) | `AbstractRawFirBuilder<T>` already abstracts correctly. A third impl for SyntaxTree would follow the pattern. |
| **java-direct** | Yes (main subject) | `JavaSyntaxNode` → `JavaLightTree`. Independent of FIR abstraction work. |
| **KMP Kotlin parser** | Needs maturation | Must reach production parity before it can serve as FIR input. |
| **Diagnostic positioning** | Yes (hardest part) | 136 `FlyweightCapableTreeStructure` refs in `LightTreePositioningStrategies.kt` need abstracting. |

---

*Last updated: 2026-04-20*
