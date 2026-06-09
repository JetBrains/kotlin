# java-direct LightTree Migration Plan

**Date**: 2026-04-20
**Scope**: Migration from materialized `JavaSyntaxNode` tree to flat-array `JavaLightTree` representation
**Approach**: Custom tree adapter over `SyntaxTreeBuilder` output (sections 4.1/4.2 of LIGHTTREE_MIGRATION_ANALYSIS.md)
**Node design**: Option A (value class, less allocations) with precomputed `parentIndex[]`

---

## Table of Contents

1. [Architectural Design](#1-architectural-design)
2. [Data Layout and Encoding](#2-data-layout-and-encoding)
3. [Step-by-Step Implementation Plan](#3-step-by-step-implementation-plan)
4. [Detailed File Change List](#4-detailed-file-change-list)
5. [Navigation API Design](#5-navigation-api-design)
6. [Model Class Migration Patterns](#6-model-class-migration-patterns)
7. [Test Strategy](#7-test-strategy)
8. [Risk Register and Mitigations](#8-risk-register-and-mitigations)

---

## 1. Architectural Design

### 1.1 Current Flow (to be replaced)

```
Source → SyntaxTreeBuilder → prepareProduction() → ProductionMarkerList + TokenList
                                                          │
                                                    buildSyntaxTree()    ← ELIMINATED
                                                          │
                                                    JavaSyntaxNode tree  ← ELIMINATED
                                                          │
                                                    model classes (*OverAst)
```

### 1.2 New Flow

```
Source → SyntaxTreeBuilder → prepareProduction() → ProductionMarkerList + TokenList
                                                          │
                                                    JavaLightTree(markers, tokens, source)
                                                          │
                                                    model classes (*OverAst)
                                                    (hold JavaLightNode + JavaLightTree ref)
```

### 1.3 Key New Types

| Type | Kind | Purpose |
|------|------|---------|
| `JavaLightTree` | Class | Wraps `ProductionMarkerList` + `TokenList` + `CharSequence`. All navigation operates through this. |
| `JavaLightNode` | `@JvmInline value class` | An `Int` index encoding either a composite marker or a token. Zero allocation when used inline. |

### 1.4 Why Option A (value class)

The analysis document notes that Option B (sealed class with `Composite`/`Token` subclasses) is "clearer but allocates on every `getChildren()` call." For java-direct, `getChildren()` is called on every lazy property access in model classes, and these model classes are retained for the entire compilation session. Option A avoids:
- Object allocation in every `getChildren()`, `findChildByType()`, `getChildrenByType()` call
- GC pressure from millions of short-lived wrapper objects across 500+ files
- Sealed class dispatch overhead on every node operation

The disambiguation between composite markers and tokens is handled by encoding conventions in the `Int` value (see Section 2).

---

## 2. Data Layout and Encoding

### 2.1 Node Index Encoding

A `JavaLightNode` is a single `Int` that encodes both the node kind and its index:

```
Positive values [0 .. markerCount-1]:  composite marker index (into ProductionMarkerList)
Negative values [-1 .. -tokenCount]:   token index (encoded as -(tokenIndex + 1))
```

This uses the full `Int` range without collision:
- `JavaLightNode(0)` = composite marker at index 0
- `JavaLightNode(-1)` = token at index 0
- `JavaLightNode(-2)` = token at index 1

Helper properties on `JavaLightTree`:
```kotlin
fun isComposite(node: JavaLightNode): Boolean = node.index >= 0
fun isToken(node: JavaLightNode): Boolean = node.index < 0
fun tokenIndex(node: JavaLightNode): Int = -(node.index + 1)     // only valid if isToken
fun markerIndex(node: JavaLightNode): Int = node.index             // only valid if isComposite
```

### 2.2 Precomputed Arrays

Built once during `JavaLightTree` construction (single pass over `ProductionMarkerList`):

| Array | Type | Size | Purpose |
|-------|------|------|---------|
| `parentMarkerIndex` | `IntArray` | one per done-marker | Maps each done-marker index → its parent's done-marker index. `-1` for root. |
| `doneForStart` | `IntArray` | one per start-marker | Maps each start-marker index → its corresponding done-marker index. Enables jumping from start to done in O(1). |

These arrays are computed from the flat marker list's nesting structure. The construction algorithm is identical to what `buildSyntaxTree()` already does with its stack — except instead of materializing nodes, we record parent indices.

**Construction algorithm:**
```
stack = [ROOT_INDEX]
for i in 0 until productionMarkers.size:
    if isDoneMarker(i):
        parentMarkerIndex[i] = stack.peek()
        stack.pop()                          // pop the corresponding start
    elif isErrorMarker(i):
        parentMarkerIndex[i] = stack.peek()  // error markers are leaf children of current
    else:
        // start marker — its parent is the current top of stack (its done)
        stack.push(i)
        doneForStart[i] = <to be filled when we see the done>
```

**Note:** The exact algorithm depends on how `ProductionMarkerList` orders start/done pairs. The existing `buildSyntaxTree()` in `utils.kt:119-146` already handles this correctly — the precomputation mirrors that logic but stores indices instead of building nodes.

### 2.3 Memory Budget

For a 1000-line Java file (~400 composite markers, ~3000 tokens):
- `ProductionMarkerList`: ~9.6 KB (400 markers × 24 bytes, already exists)
- `TokenList`: ~24 KB (3000 tokens × 8 bytes, already exists)
- `parentMarkerIndex`: ~1.6 KB (400 × 4 bytes, **new**)
- `doneForStart`: ~0.8 KB (200 starts × 4 bytes, **new**)
- `source`: ~2 KB (shared `CharSequence`, already exists)
- **Total new overhead**: ~2.4 KB vs. ~216 KB for materialized `JavaSyntaxNode` tree

---

## 3. Step-by-Step Implementation Plan

### Phase 1: Infrastructure — New Light Tree Classes

**Goal:** Implement `JavaLightNode`, `JavaLightTree`, and navigation extensions in a new file, without touching any existing code.

#### Step 1.1: Create `JavaLightTree.kt`

New file: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaLightTree.kt`

Contents:
1. `JavaLightNode` — `@JvmInline value class` wrapping `Int`
2. `JavaLightTree` — main class holding:
   - `val productionMarkers: ProductionMarkerList`
   - `val tokens: TokenList`
   - `val source: CharSequence`
   - `val parentMarkerIndex: IntArray` (precomputed)
   - `val doneForStart: IntArray` (precomputed, maps start-marker → done-marker)
   - `val startForDone: IntArray` (precomputed, maps done-marker → start-marker)
3. Core methods:
   - `fun getRoot(): JavaLightNode`
   - `fun getType(node: JavaLightNode): SyntaxElementType`
   - `fun getStartOffset(node: JavaLightNode): Int`
   - `fun getEndOffset(node: JavaLightNode): Int`
   - `fun getText(node: JavaLightNode): CharSequence` (returns substring view, no copy)
   - `fun textEquals(node: JavaLightNode, expected: String): Boolean` (char-by-char, no allocation)
   - `fun getParent(node: JavaLightNode): JavaLightNode?`
   - `fun getChildren(node: JavaLightNode): List<JavaLightNode>` (returns lightweight list)
   - `fun isComposite(node: JavaLightNode): Boolean`

#### Step 1.2: Implement `getChildren()`

This is the most complex method. For a composite done-marker at index `d`:
1. Find the corresponding start-marker index `s = startForDone[d]`.
2. Get the token range: `startToken = marker[s].getStartTokenIndex()`, `endToken = marker[d].getEndTokenIndex()`.
3. Scan forward from `s+1`, collecting:
   - **Child composites**: done-markers where `parentMarkerIndex[child] == d`. Skip their entire span (jump from start to done using `doneForStart`).
   - **Child error markers**: error-markers where `parentMarkerIndex[error] == d`.
   - **Child tokens**: tokens in gaps between child composites/errors. Tokens with `start == end` are skipped (empty tokens like DANGLING_NEWLINE).

Return as `JavaLightNodeList` (a lightweight list-like wrapper around an `IntArray` to avoid `List<JavaLightNode>` boxing issues with value classes — or return `IntArray` directly with a size).

**Alternative simpler approach:** Since `getChildren()` is computed on-demand and model classes iterate children sequentially, we can return an `ArrayList<JavaLightNode>` initially. The value class boxing cost is one `Int` per list element — negligible compared to the 72-byte `JavaSyntaxNode` objects we're eliminating.

#### Step 1.3: Implement navigation extension functions

These mirror the current `JavaSyntaxNode` extensions and will minimize mechanical changes in model classes:

```kotlin
// In JavaLightTree.kt or as extensions

fun JavaLightTree.findChildByType(node: JavaLightNode, type: SyntaxElementType): JavaLightNode?
fun JavaLightTree.findChildByType(node: JavaLightNode, typeName: String): JavaLightNode?
fun JavaLightTree.getChildrenByType(node: JavaLightNode, type: SyntaxElementType): List<JavaLightNode>
fun JavaLightTree.getChildrenByType(node: JavaLightNode, typeName: String): List<JavaLightNode>
fun JavaLightTree.hasChildOfType(node: JavaLightNode, type: SyntaxElementType): Boolean
fun JavaLightTree.childrenIterator(node: JavaLightNode): Iterator<JavaLightNode>
fun JavaLightTree.childrenSequence(node: JavaLightNode): Sequence<JavaLightNode>
fun JavaLightTree.getParent(node: JavaLightNode): JavaLightNode?
fun JavaLightTree.getSiblings(node: JavaLightNode): List<JavaLightNode>  // for multi-field declarations
```

**Important:** `findChildByType` and `hasChildOfType` can be optimized to **not** build the full children list — they can scan markers/tokens in the range and return early on first match.

#### Step 1.4: Create `buildJavaLightTree()` factory function

In `parse.kt` (or as a new top-level function in `JavaLightTree.kt`):

```kotlin
fun buildJavaLightTree(builder: SyntaxTreeBuilder, source: CharSequence): JavaLightTree {
    val production = prepareProduction(builder)
    val markers = production.productionMarkers
    val tokens = builder.tokens
    // Build parentMarkerIndex, doneForStart, startForDone arrays
    val (parentIdx, doneForStart, startForDone) = buildParentIndex(markers)
    return JavaLightTree(markers, tokens, source, parentIdx, doneForStart, startForDone)
}
```

#### Step 1.5: Unit tests for `JavaLightTree`

New test file: `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaLightTreeTest.kt`

Tests:
- Parse a simple Java class, verify `getRoot()` type is `JAVA_CODE` or `FILE`
- Verify `getChildren(root)` contains `PACKAGE_STATEMENT`, `IMPORT_LIST`, `CLASS` nodes
- Verify `findChildByType(classNode, IDENTIFIER)` returns the class name token
- Verify `getText()` returns correct text for both composite and token nodes
- Verify `textEquals()` matches correctly
- Verify `getParent()` returns the correct parent for deeply nested nodes
- Verify children of a token node return empty list

### Phase 2: Parallel Verification

**Goal:** Run both representations side by side and assert equivalence before committing to the new one.

#### Step 2.1: Add dual-mode to `JavaParsingTestBase`

Extend `parseSource()` to build both trees and verify equivalence:

```kotlin
protected fun parseSource(source: String): Pair<JavaSyntaxNode, JavaResolutionContext> {
    val builder = parseJavaToSyntaxTreeBuilder(source, 0)
    val root = buildSyntaxTree(builder, source)

    // TEMPORARY: verify light tree produces equivalent structure
    val builder2 = parseJavaToSyntaxTreeBuilder(source, 0)
    val lightTree = buildJavaLightTree(builder2, source)
    assertTreesEquivalent(root, lightTree, lightTree.getRoot())

    val context = JavaResolutionContext.create(root)
    return root to context
}
```

#### Step 2.2: Implement `assertTreesEquivalent()`

Recursively compare:
- Type of each node matches
- Start/end offsets match
- Text content matches
- Number of children matches (excluding whitespace consistently)
- Recurse into children

This catches any bugs in `getChildren()`, `parentIndex`, or token-gap handling before they propagate into model classes.

#### Step 2.3: Run the full existing test suite

All 9 test files should pass with dual-mode verification active. Fix any discrepancies found.

### Phase 3: Model Class Migration

**Goal:** Migrate all `*OverAst` classes from `JavaSyntaxNode` to `JavaLightNode` + `JavaLightTree`.

#### Step 3.1: Migrate `JavaElementOverAst` (base class)

**Current** (`JavaElementOverAst.kt:10-19`):
```kotlin
abstract class JavaElementOverAst(
    val node: JavaSyntaxNode
) : JavaElement
```

**New:**
```kotlin
abstract class JavaElementOverAst(
    val node: JavaLightNode,
    val tree: JavaLightTree,
) : JavaElement {
    override fun equals(other: Any?): Boolean =
        other is JavaElementOverAst && node == other.node && tree === other.tree

    override fun hashCode(): Int = node.hashCode()

    override fun toString(): String = tree.getType(node).toString()
}
```

**Note:** `equals` compares node indices AND tree identity (same parsed file). This is important because the same marker index in two different files would otherwise collide. Using `===` for tree comparison is correct because there's one `JavaLightTree` per parsed file.

**Impact:** Every subclass constructor changes. This is the domino that triggers all downstream changes.

#### Step 3.2: Migrate `JavaTypeOverAst` (base class for types)

**Current:**
```kotlin
abstract class JavaTypeOverAst(
    val node: JavaSyntaxNode,
    protected val resolutionContext: JavaResolutionContext,
    ...
)
```

**New:**
```kotlin
abstract class JavaTypeOverAst(
    val node: JavaLightNode,
    val tree: JavaLightTree,
    protected val resolutionContext: JavaResolutionContext,
    ...
)
```

This class does NOT extend `JavaElementOverAst` — it has its own `node` field. Must add `tree` parameter.

#### Step 3.3: Migrate `JavaClassOverAst`

This is the largest model class (468 lines, 20+ node access sites).

**Mechanical transformation pattern for each access site:**

| Current pattern | New pattern |
|----------------|-------------|
| `node.children.find { it.type == X }` | `tree.findChildByType(node, X)` |
| `node.findChildByType(X)` | `tree.findChildByType(node, X)` |
| `node.getChildrenByType(X)` | `tree.getChildrenByType(node, X)` |
| `node.children.filter { ... }` | `tree.getChildren(node).filter { ... }` |
| `node.children.any { it.type == X }` | `tree.hasChildOfType(node, X)` |
| `someNode.text` | `tree.getText(someNode).toString()` (or use `textEquals` where possible) |
| `someNode.textEquals(s)` | `tree.textEquals(someNode, s)` |
| `node.children` (direct iteration) | `tree.getChildren(node)` |
| `node.type` | `tree.getType(node)` |
| `node.parent` | `tree.getParent(node)` |

**Specific high-complexity spots in `JavaClassOverAst`:**

1. **`name` property** (line 46): `node.children.find { it.type == JavaSyntaxTokenType.IDENTIFIER }?.text`
   → `tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() }`

2. **`hasModifier()` helper** (line 82-84): `modifierList?.children?.any { it.type == modifier }`
   → `modifierList?.let { tree.hasChildOfType(it, modifier) } ?: false`

3. **`computeIsAnnotationType()`** (lines 312-325): Iterates `node.children` with index-based adjacency check for `AT` → `INTERFACE_KEYWORD`.
   → Same pattern but using `tree.getChildren(node)` and `tree.getType(child)`.

4. **`findInnerClassUncached()`** (lines 187-277): Complex — iterates `node.children`, calls `findChildByType` on child nodes, checks modifiers.
   → Mechanical: replace all `node.children` with `tree.getChildren(node)`, all `childNode.findChildByType(X)` with `tree.findChildByType(childNode, X)`.

5. **`findInnerClassInSupertypes()`** (lines 242-278): Accesses `supertypeClass.node.children` — accessing another `JavaClassOverAst`'s node.
   → After migration, `supertypeClass.node` is a `JavaLightNode` and `supertypeClass.tree` provides the light tree. This is already correct since each `JavaClassOverAst` carries both `node` and `tree`.

6. **`permittedTypes` / `deriveImplicitPermittedTypes()`** (lines 357-394): Uses `node.children.filter { ... }` and chained `findChildByType` / `getChildrenByType`.
   → Mechanical transformation.

7. **Inner class construction** (line 215): `JavaClassOverAst(innerClassNode, contextForInner, outerClass = this)`
   → `JavaClassOverAst(innerClassNode, tree, contextForInner, outerClass = this)` — must pass `tree`.

#### Step 3.4: Migrate `JavaMemberOverAst` and subclasses

**`JavaMemberOverAst`** (89 lines, base for Field/Method/Constructor):
- Constructor: add `tree: JavaLightTree` parameter, pass to `JavaElementOverAst`
- `hasModifier()`: same pattern as `JavaClassOverAst`

**`JavaFieldOverAst`** (233 lines):
- **`computeLeadingFieldNode()`** (lines 120-135): Uses `node.parent` and `parent.children` + `siblings.indexOf(node)`.
  → `tree.getParent(node)` for parent. For siblings: `tree.getChildren(parentNode)`.
  → For `indexOf`: need to find the index of `node` in the children list. Since `JavaLightNode` is a value class wrapping `Int`, equality comparison is cheap.

- **`initializerNode`** (lines 202-212): `node.children` iteration + `indexOfFirst` + `drop(eqIndex + 1)`.
  → `tree.getChildren(node)` + same sequential scan.

**`JavaMethodOverAst`** (103 lines):
- `annotationParameterDefaultValue` (lines 402-421): Same `node.children` + `indexOfFirst` + `drop()` pattern.

**`JavaConstructorOverAst`** (44 lines): Straightforward — same patterns as method.

**`JavaValueParameterOverAst`** (37 lines): Simple `findChildByType` calls.

#### Step 3.5: Migrate `JavaTypeOverAst` and subclasses

**`JavaClassifierTypeOverAst`** (351 lines) — the most complex:
- **`collectIdentifiers()`** (lines 125-133): Recursive iteration of `n.children`, checking `child.type`.
  → `tree.getChildren(n)` iteration, `tree.getType(child)` checks. Must pass `tree` as parameter (or it's available via the class field).

- **`collectAllRefParamLists()`** (lines 361-370): Same recursive pattern.
  → Must receive `tree` parameter since it operates on arbitrary nodes, not just `this.node`.

- **`computeIsRaw()`** (line 283): `node.findChildByType(...)?.children?.count { it.type == TYPE }`
  → `tree.findChildByType(node, REFERENCE_PARAMETER_LIST)?.let { tree.getChildren(it).count { tree.getType(it) == TYPE } }`

**`JavaPrimitiveTypeOverAst`** (line 448-467): Uses `node.text` for type keyword matching.
  → `tree.getText(node).toString()` or preferably a chain of `tree.textEquals(node, "int")` checks.

**`createJavaType()`** function (lines 507-619): Heavy use of `node.findChildByType()`, `node.children.count { }`, `node.getChildrenByType()`.
  → All become `tree.findChildByType(node, ...)`, `tree.getChildren(node).count { ... }`, `tree.getChildrenByType(node, ...)`.
  → Must add `tree: JavaLightTree` parameter to `createJavaType()` and `createJavaTypeWithAnnotations()`.

**`JavaTypeParameterOverAst`** (lines 642-704): Standard `findChildByType`/`getChildrenByType` patterns.

#### Step 3.6: Migrate `JavaAnnotationOverAst` and annotation argument classes

**`JavaAnnotationOverAst`** (77 lines): `findChildByType`/`getChildrenByType` patterns.

**Annotation argument classes** (lines 78-375): Several classes hold a `JavaSyntaxNode` directly (not via `JavaElementOverAst`):
- `JavaArrayAnnotationArgumentOverAst(val arrayNode: JavaSyntaxNode, ...)`
- `JavaEnumValueAnnotationArgumentOverAst(val refNode: JavaSyntaxNode, ...)`
- `JavaClassObjectAnnotationArgumentOverAst(val classObjNode: JavaSyntaxNode, ...)`
- `JavaAnnotationAsAnnotationArgumentOverAst(val annotationNode: JavaSyntaxNode, ...)`

Each of these needs to hold `JavaLightNode` + `JavaLightTree` instead.

**`createAnnotationArgument()` and `createAnnotationArgumentFromValue()`**: Must add `tree: JavaLightTree` parameter.

#### Step 3.7: Migrate `ConstantEvaluator`

**`ConstantEvaluator`** (293 lines): Operates on `JavaSyntaxNode` throughout.
- `evaluate(node: JavaSyntaxNode)` → `evaluate(node: JavaLightNode)`
- All `node.children` → `tree.getChildren(node)`
- All `node.type` → `tree.getType(node)`
- All `node.text` → `tree.getText(node).toString()`
- Must hold `tree: JavaLightTree` as constructor parameter or method parameter.

Since `ConstantEvaluator` is constructed with `containingClass: JavaClassOverAst`, and `JavaClassOverAst` will carry `tree`, the evaluator can access `tree` through `containingClass.tree`.

But `evaluate()` also receives `JavaSyntaxNode` parameters from callers (e.g., `initializerNode`). These all become `JavaLightNode`.

#### Step 3.8: Migrate `JavaImportResolver`

**`JavaImportResolver`** (264 lines): Stateless object with methods operating on `JavaSyntaxNode` parameters.

- **`importCache`**: Currently `MutableMap<JavaSyntaxNode, ...>` (weak-keyed).
  → Must change key type. Options:
  - **Option A**: Key by `JavaLightTree` reference (identity). One light tree per file, cached imports per file. `WeakHashMap<JavaLightTree, ...>`.
  - **Option B**: Key by `JavaLightNode` (root node index). But value class wrapping `Int` can't be a weak reference key.
  → **Use Option A**: `WeakHashMap<JavaLightTree, ...>`. The `JavaLightTree` instance already uniquely identifies a parsed file.

- All methods: `root: JavaSyntaxNode` → `root: JavaLightNode, tree: JavaLightTree` (or just `tree: JavaLightTree` since root can be `tree.getRoot()`).
- All `node.findChildByType()` → `tree.findChildByType(node, ...)`
- All `node.getChildrenByType()` → `tree.getChildrenByType(node, ...)`
- All `node.children` iteration → `tree.getChildren(node)` iteration

#### Step 3.9: Migrate `JavaResolutionContext`

**`JavaResolutionContext`** (622 lines): Holds a reference to the AST root indirectly (through `localClassProvider`).

- The `create()` factory takes `root: JavaSyntaxNode`. → Change to `root: JavaLightNode, tree: JavaLightTree` (or just `tree: JavaLightTree`).
- Internal `localClassProvider` returns `JavaClass?` — this is already abstract, no change needed.
- `extractImports(root: JavaSyntaxNode)` → delegate to `JavaImportResolver` with light tree.

#### Step 3.10: Migrate `JavaSupertypeGraph`

**`JavaSupertypeGraph`** (246 lines):
- `getDirectSupertypes()` slow path: calls `buildSyntaxTree()` → change to `buildJavaLightTree()`.
- `getInnerClassNames()` slow path: same.
- `extractSupertypeRefsFromNode()`: Takes `classNode: JavaSyntaxNode` → `classNode: JavaLightNode, tree: JavaLightTree`.
- `findClassInTree()`: Same parameter change.

#### Step 3.11: Migrate `JavaClassFinderOverAstImpl`

**`JavaClassFinderOverAstImpl`** (385 lines):
- `tryBuildFileEntryWithFullParse()`: Replace `buildSyntaxTree()` with `buildJavaLightTree()`. All subsequent node operations use light tree API.
- `parseTopLevelClassFromFile()`: Same.
- `indexPackageInfo()`: Same.

### Phase 4: Cleanup and Finalization

#### Step 4.1: Remove `JavaSyntaxNode` and `buildSyntaxTree()`

Delete from `utils.kt`:
- `JavaSyntaxNode` class (lines 15-98)
- `buildSyntaxTree()` function (lines 100-155)
- `MutableList<T>.peek()` extension (line 157)
- `findChildByType(typeName: String)` extension (lines 159-161)
- `getChildrenByType(typeName: String)` extension (lines 163-165)
- `CHILD_INDEX_THRESHOLD` constant (line 171)
- `BELOW_THRESHOLD_SENTINEL` (line 179)
- `findChildByType(type: SyntaxElementType)` extension (lines 181-184)
- `getChildrenByType(type: SyntaxElementType)` extension (lines 186-189)

Keep:
- `computeTypeParameters()` function (lines 191-210) — update to use `JavaLightNode` + `JavaLightTree`.

#### Step 4.2: Remove parallel verification code

Remove `assertTreesEquivalent()` from `JavaParsingTestBase` and the dual-build logic.

#### Step 4.3: Move `JavaLightTree` and related types

Consider whether `JavaLightTree.kt` should be split:
- `JavaLightNode` and `JavaLightTree` core → `JavaLightTree.kt`
- Navigation extensions → same file or `JavaLightTreeUtils.kt`

Keep in one file if total is under ~400 lines.

#### Step 4.4: Update `parse.kt`

Change `parseJavaToSyntaxTreeBuilder()` signature or add a companion:
```kotlin
fun parseJavaToLightTree(charSequence: CharSequence, start: Int): JavaLightTree {
    val builder = parseJavaToSyntaxTreeBuilder(charSequence, start)
    return buildJavaLightTree(builder, charSequence)
}
```

The original `parseJavaToSyntaxTreeBuilder()` may still be useful if any caller needs the raw builder. Evaluate during cleanup.

---

## 4. Detailed File Change List

### New Files

| File | Lines (est.) | Content |
|------|-------------|---------|
| `JavaLightTree.kt` | ~300-350 | `JavaLightNode`, `JavaLightTree`, `buildJavaLightTree()`, all navigation extensions |
| `JavaLightTreeTest.kt` | ~150-200 | Unit tests for the light tree infrastructure |

### Modified Files

| File | Current lines | Change type | Key changes |
|------|--------------|-------------|-------------|
| **`utils.kt`** | 211 | **Major rewrite** | Remove `JavaSyntaxNode`, `buildSyntaxTree()`, all extensions. Keep `computeTypeParameters()` migrated to light tree. ~150 lines removed, ~30 lines updated. |
| **`parse.kt`** | 39 | **Minor** | Add `parseJavaToLightTree()`. Keep `parseJavaToSyntaxTreeBuilder()` if needed. ~10 lines added. |
| **`JavaElementOverAst.kt`** | 19 | **Rewrite** | Constructor takes `JavaLightNode` + `JavaLightTree`. Equals/hashCode updated. |
| **`JavaClassOverAst.kt`** | 468 | **Major** | All 20+ `node.*` access sites → `tree.*` calls. Constructor gains `tree` param. ~60 lines changed. |
| **`JavaMemberOverAst.kt`** | 511 | **Major** | All 28+ access sites migrated. All subclass constructors gain `tree` param. ~80 lines changed. |
| **`JavaTypeOverAst.kt`** | 772 | **Major** | Add `tree` field. `collectIdentifiers()`, `collectAllRefParamLists()` migrated. `createJavaType()` gains `tree` param. ~100 lines changed. |
| **`JavaAnnotationOverAst.kt`** | 375 | **Moderate** | `createAnnotationArgument()` / `createAnnotationArgumentFromValue()` gain `tree` param. All node-holding argument classes gain `tree`. ~40 lines changed. |
| **`ConstantEvaluator.kt`** | 293 | **Moderate** | All `node.children`/`.text`/`.type` → tree API. Constructor or access via `containingClass.tree`. ~50 lines changed. |
| **`JavaImportResolver.kt`** | 264 | **Moderate** | Cache key changes from `JavaSyntaxNode` to `JavaLightTree`. All method signatures gain `tree` param. ~40 lines changed. |
| **`JavaResolutionContext.kt`** | 622 | **Low-Moderate** | `create()` factory params change. Internal `localClassProvider` may need light tree access. ~15 lines changed. |
| **`JavaClassFinderOverAstImpl.kt`** | 385 | **Moderate** | All `buildSyntaxTree()` calls → `buildJavaLightTree()`. Node operations on parsed roots → tree API. ~30 lines changed. |
| **`JavaRecordComponentOverAst.kt`** | 33 | **Low** | Constructor gains `tree` param. 3 `findChildByType` calls migrated. ~8 lines changed. |
| **`JavaSupertypeGraph.kt`** | 246 | **Moderate** | Slow-path `buildSyntaxTree()` → `buildJavaLightTree()`. `extractSupertypeRefsFromNode()` parameters change. ~25 lines changed. |
| **`JavaScopeResolver.kt`** | ~140 | **Low** | May need light tree access for local class construction. ~5 lines changed. |
| **`JavaParsingTestBase.kt`** | 37 | **Moderate** | `parseSource()` uses light tree. `parseFirstClass()` updated. Phase 2 dual-mode, then Phase 4 cleanup. |
| **Other test files** (8 files) | ~2000 total | **Low-Moderate** | Tests exercise model classes, not AST directly. Changes are mostly in `parseFirstClass()` and any tests that inspect AST structure. ~50-80 lines total. |

### Unchanged Files

| File | Lines | Reason |
|------|-------|--------|
| `JavaPackageOverAst.kt` | 33 | No AST access |
| `JavaSourceIndex.kt` | 115 | Operates on file entries |
| `JavaSourceFileReader.kt` | — | I/O only |
| `JavaLiteralParser.kt` | — | String/number parsing, no AST |
| `JavaInheritedMemberResolver.kt` | — | Operates on `JavaClass` interface, not AST |

### Summary

| Category | Lines changed (est.) | Files |
|----------|---------------------|-------|
| New infrastructure | ~350-450 new | 2 new files |
| `utils.kt` cleanup | ~150 removed, ~30 changed | 1 file |
| Base class rewrites | ~30 changed | 2 files (`JavaElementOverAst`, `JavaTypeOverAst` base) |
| Model class migration | ~350-400 changed | 7 files |
| Supporting class migration | ~90 changed | 4 files (`JavaImportResolver`, `ConstantEvaluator`, `JavaResolutionContext`, `JavaSupertypeGraph`) |
| Finder + parse | ~40 changed | 2 files |
| Tests | ~100-150 changed | 9 test files |
| **Total** | **~350-450 new + 800-900 changed** | **~18 files** |

---

## 5. Navigation API Design

### 5.1 Core Methods on `JavaLightTree`

```kotlin
@JvmInline
value class JavaLightNode(val index: Int)

class JavaLightTree(
    private val markers: ProductionMarkerList,
    private val tokens: TokenList,
    val source: CharSequence,
    private val parentMarkerIndex: IntArray,
    private val doneForStart: IntArray,
    private val startForDone: IntArray,
) {
    fun getRoot(): JavaLightNode

    // Core node properties
    fun getType(node: JavaLightNode): SyntaxElementType
    fun getStartOffset(node: JavaLightNode): Int
    fun getEndOffset(node: JavaLightNode): Int
    fun getText(node: JavaLightNode): CharSequence   // subSequence, no copy
    fun textEquals(node: JavaLightNode, expected: String): Boolean

    // Tree navigation
    fun getChildren(node: JavaLightNode): List<JavaLightNode>
    fun getParent(node: JavaLightNode): JavaLightNode?
    fun getSiblings(node: JavaLightNode): List<JavaLightNode>

    // Optimized child search (no full children list)
    fun findChildByType(node: JavaLightNode, type: SyntaxElementType): JavaLightNode?
    fun findChildByType(node: JavaLightNode, typeName: String): JavaLightNode?
    fun getChildrenByType(node: JavaLightNode, type: SyntaxElementType): List<JavaLightNode>
    fun getChildrenByType(node: JavaLightNode, typeName: String): List<JavaLightNode>
    fun hasChildOfType(node: JavaLightNode, type: SyntaxElementType): Boolean

    // Utility
    fun isComposite(node: JavaLightNode): Boolean
    fun isToken(node: JavaLightNode): Boolean
}
```

### 5.2 Why `findChildByType` is a Method, Not an Extension

In the current design, `findChildByType` is an extension on `JavaSyntaxNode`. In the new design, it must be a method (or extension) on `JavaLightTree` because the tree holds the data arrays. Making these methods directly on `JavaLightTree` avoids the need to pass `tree` separately in every call — the call site reads `tree.findChildByType(node, type)` which is clean and consistent.

### 5.3 Handling the `childByTypeIndex` Optimization

The current `JavaSyntaxNode` has a `cachedChildByTypeIndex` map for nodes with >4 children. This optimization is not needed in the light tree approach because:

1. `findChildByType()` scans the marker/token range directly — this is O(k) where k = children count, same as building the index.
2. The index was primarily valuable because `JavaSyntaxNode` persisted for the entire compilation and was queried multiple times. With light tree, the scan cost is paid only on each call, but:
   - Most properties are cached with `@Volatile` — each `findChildByType` call happens at most once per property per model instance.
   - The scan is over flat arrays with excellent cache locality, vs. the old approach which chased pointers through a `List<JavaSyntaxNode>`.
3. If profiling shows a hotspot, a per-node `Map` can be added as a `ConcurrentHashMap<JavaLightNode, Map<SyntaxElementType, JavaLightNode>>` on `JavaLightTree`. But this is unlikely to be needed.

---

## 6. Model Class Migration Patterns

### 6.1 Constructor Pattern

**Before:**
```kotlin
class JavaClassOverAst(
    node: JavaSyntaxNode,
    val resolutionContext: JavaResolutionContext,
    override val outerClass: JavaClass? = null,
) : JavaElementOverAst(node), JavaClass
```

**After:**
```kotlin
class JavaClassOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    val resolutionContext: JavaResolutionContext,
    override val outerClass: JavaClass? = null,
) : JavaElementOverAst(node, tree), JavaClass
```

### 6.2 Simple Property Pattern

**Before:**
```kotlin
override val name: Name
    get() = Name.identifier(node.children.find { it.type == IDENTIFIER }?.text ?: "<error>")
```

**After:**
```kotlin
override val name: Name
    get() = Name.identifier(
        tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)
            ?.let { tree.getText(it).toString() } ?: "<error>"
    )
```

### 6.3 Cached Property Pattern (modifier list)

**Before:**
```kotlin
@Volatile private var _modifierList: Any? = NOT_COMPUTED
private val modifierList: JavaSyntaxNode?
    get() {
        val cached = _modifierList
        if (cached !== NOT_COMPUTED) return cached as JavaSyntaxNode?
        val computed = node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)
        _modifierList = computed
        return computed
    }
```

**After:**
```kotlin
@Volatile private var _modifierList: Any? = NOT_COMPUTED
private val modifierList: JavaLightNode?
    get() {
        val cached = _modifierList
        if (cached !== NOT_COMPUTED) return cached as JavaLightNode?  // value class — boxed in Any?
        val computed = tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
        _modifierList = computed  // boxes the Int; or store as Int sentinel
        return computed
    }
```

**Note on value class boxing:** When `JavaLightNode` (a value class wrapping `Int`) is stored in `Any?`, it gets boxed. This is acceptable — one `Integer` box per cached property, same as today's one reference. Alternatively, store as `Int` with `-2` as NOT_COMPUTED sentinel (since `-1` is valid token index 0).

### 6.4 `hasModifier()` Pattern

**Before:**
```kotlin
private fun hasModifier(modifier: SyntaxElementType): Boolean {
    return modifierList?.children?.any { it.type == modifier } ?: false
}
```

**After:**
```kotlin
private fun hasModifier(modifier: SyntaxElementType): Boolean {
    return modifierList?.let { tree.hasChildOfType(it, modifier) } ?: false
}
```

### 6.5 Children Iteration Pattern

**Before:**
```kotlin
val children = node.children
val eqIndex = children.indexOfFirst { it.type == JavaSyntaxTokenType.EQ }
if (eqIndex < 0) return null
return children.drop(eqIndex + 1).firstOrNull {
    it.type != SyntaxTokenTypes.WHITE_SPACE && it.type != JavaSyntaxTokenType.SEMICOLON
}
```

**After:**
```kotlin
val children = tree.getChildren(node)
val eqIndex = children.indexOfFirst { tree.getType(it) == JavaSyntaxTokenType.EQ }
if (eqIndex < 0) return null
return children.drop(eqIndex + 1).firstOrNull {
    tree.getType(it) != SyntaxTokenTypes.WHITE_SPACE && tree.getType(it) != JavaSyntaxTokenType.SEMICOLON
}
```

### 6.6 Recursive Traversal Pattern

**Before** (`collectIdentifiers` in `JavaClassifierTypeOverAst`):
```kotlin
private fun collectIdentifiers(n: JavaSyntaxNode, parts: MutableList<String>) {
    for (child in n.children) {
        when (child.type) {
            JavaSyntaxTokenType.IDENTIFIER -> parts.add(child.text)
            JavaSyntaxElementType.JAVA_CODE_REFERENCE -> collectIdentifiers(child, parts)
        }
    }
}
```

**After:**
```kotlin
private fun collectIdentifiers(n: JavaLightNode, parts: MutableList<String>) {
    for (child in tree.getChildren(n)) {
        when (tree.getType(child)) {
            JavaSyntaxTokenType.IDENTIFIER -> parts.add(tree.getText(child).toString())
            JavaSyntaxElementType.JAVA_CODE_REFERENCE -> collectIdentifiers(child, parts)
        }
    }
}
```

### 6.7 Parent Navigation Pattern (multi-field declarations)

**Before** (`JavaFieldOverAst.computeLeadingFieldNode()`):
```kotlin
val parent = node.parent ?: return null
val siblings = parent.children
val myIndex = siblings.indexOf(node)
return (myIndex - 1 downTo 0)
    .map { siblings[it] }
    .firstOrNull { it.type == FIELD && ... }
```

**After:**
```kotlin
val parentNode = tree.getParent(node) ?: return null
val siblings = tree.getChildren(parentNode)
val myIndex = siblings.indexOfFirst { it == node }
return (myIndex - 1 downTo 0)
    .map { siblings[it] }
    .firstOrNull { tree.getType(it) == FIELD && ... }
```

### 6.8 Object Construction Pattern

**Before:**
```kotlin
JavaClassOverAst(innerClassNode, contextForInner, outerClass = this)
```

**After:**
```kotlin
JavaClassOverAst(innerClassNode, tree, contextForInner, outerClass = this)
```

Every `*OverAst` construction site must pass `tree`. Since `tree` is available on the containing class (via `this.tree`), this is always accessible at construction sites.

---

## 7. Test Strategy

### 7.1 Phase 1 Tests (Infrastructure)

| Test | What it verifies |
|------|-----------------|
| Parse simple class, verify root type | `getRoot()`, `getType()` |
| Verify children of root | `getChildren()` produces correct composites + tokens |
| Verify `findChildByType` | Returns correct node, returns null for missing |
| Verify `getChildrenByType` | Returns correct list, returns empty for missing |
| Verify `hasChildOfType` | True/false cases |
| Verify `getText()` | Correct text for composites and tokens |
| Verify `textEquals()` | Positive and negative cases |
| Verify `getParent()` | Returns correct parent for nested nodes |
| Verify `getParent()` of root | Returns null |
| Verify children of token | Returns empty |
| Verify deeply nested navigation | CLASS → MODIFIER_LIST → ANNOTATION → JAVA_CODE_REFERENCE |
| Parse file with multiple classes | Each class found as child of root |
| Parse file with errors | Error markers handled correctly |
| Memory: no `JavaSyntaxNode` allocation | Verify `buildJavaLightTree()` returns without creating any `JavaSyntaxNode` |

### 7.2 Phase 2 Tests (Parallel Verification)

Run all 9 existing test files with dual-mode assertions enabled. This provides ~100+ test cases of real Java source code verifying structural equivalence.

### 7.3 Phase 3 Tests (Model Class Migration)

All existing tests continue to pass. No new test cases needed — the existing tests exercise all model class properties and behavior.

### 7.4 Phase 4 Tests (Post-Cleanup)

Remove dual-mode assertions. All tests still pass. Optionally add a benchmark test comparing memory allocation between old and new approaches.

---

## 8. Risk Register and Mitigations

| # | Risk | Severity | Probability | Mitigation |
|---|------|----------|-------------|------------|
| 1 | **Value class boxing overhead** — `JavaLightNode` boxed when stored in `Any?` (cached properties), `List<JavaLightNode>`, or `Map` keys | Low | High (boxing will happen) | Boxing cost is one `Integer` object (~16 bytes) — trivial vs. saved 72-byte `JavaSyntaxNode`. For `List<JavaLightNode>`, consider using `IntArray`-backed list to avoid boxing entirely in hot paths. |
| 2 | **`getChildren()` recomputation** — called multiple times per node in some model classes | Medium | Medium | Most property accesses are cached via `@Volatile`. The children scan is O(k) over flat arrays with good cache locality. If profiling shows issues, add a `ConcurrentHashMap<Int, List<JavaLightNode>>` children cache on `JavaLightTree`. |
| 3 | **`parentIndex[]` correctness** — precomputed parent indices must exactly match the nesting semantics of `buildSyntaxTree()` | High | Low | Phase 2 parallel verification catches any discrepancy. The construction algorithm is directly derived from the existing stack-based `buildSyntaxTree()`. |
| 4 | **Value class equality in collections** — `JavaLightNode` equality is just `Int` equality, but two nodes in different trees could have the same index | Medium | Low | `equals()` in `JavaElementOverAst` checks both `node == other.node` AND `tree === other.tree`. All model class equality is correct. For collections keyed by node (like `importCache`), key by `JavaLightTree` instead. |
| 5 | **`node.text` → `tree.getText(node).toString()`** — some callers use `.text` for string operations (`.contains()`, `.substringBefore()`, etc.) | Low | High | `getText()` returns `CharSequence` (substring view). For callers needing `String` operations, `.toString()` materializes the string. This is the same cost as current `JavaSyntaxNode.text` which also materializes on first access. |
| 6 | **Multi-field leading node lookup** uses `node.parent` (5 call sites) | Medium | Low | `getParent()` uses precomputed `parentMarkerIndex` — O(1) for composites. For tokens, parent is the nearest enclosing composite marker — computed by scanning `parentMarkerIndex` for the marker whose token range contains the token. Consider precomputing token-to-parent-marker for this. |
| 7 | **Thread safety of `getChildren()`** — multiple threads call `getChildren()` on the same node | Low | High | `getChildren()` is a pure function over immutable arrays — no synchronization needed. Each call produces an independent list. No shared mutable state. |
| 8 | **`JavaImportResolver.importCache` WeakHashMap key change** — `WeakHashMap<JavaLightTree, ...>` requires `JavaLightTree` to be weakly reachable | Low | Low | `JavaLightTree` is held by model classes (via `tree` field), which are held by `classCache` in `JavaClassFinderOverAstImpl`. When a class is evicted, the tree becomes unreachable and the cache entry is cleared. Same lifecycle as current `JavaSyntaxNode`-keyed cache. |
| 9 | **Token-to-parent mapping for `getParent()` on tokens** | Medium | Medium | Tokens don't have entries in `parentMarkerIndex`. To find a token's parent: binary search `markers` for the smallest composite whose token range contains the token. Or: precompute a `tokenParentIndex: IntArray` (one int per token, ~4 bytes × tokenCount). Adds ~12 KB for 3000 tokens. |

---

## Appendix A: Ordering of Implementation Work

Recommended implementation ordering for minimizing risk and enabling incremental verification:

```
Week 1:
  Step 1.1-1.4: Create JavaLightTree.kt with all core + navigation methods
  Step 1.5:     Unit tests for JavaLightTree
  Step 2.1-2.3: Parallel verification in test base, run all tests

Week 2:
  Step 3.1:     Migrate JavaElementOverAst (base class)
  Step 3.2:     Migrate JavaTypeOverAst (type base class)
  Step 3.5:     Migrate JavaTypeOverAst subclasses + createJavaType()
                (migrated early because JavaTypeOverAst does NOT extend JavaElementOverAst
                 — independent of Step 3.1's change propagation)
  Step 3.3:     Migrate JavaClassOverAst
                Run tests after each file

Week 3:
  Step 3.4:     Migrate JavaMemberOverAst + all subclasses
  Step 3.6:     Migrate JavaAnnotationOverAst + argument classes
  Step 3.7:     Migrate ConstantEvaluator
                Run tests after each file

Week 4:
  Step 3.8:     Migrate JavaImportResolver
  Step 3.9:     Migrate JavaResolutionContext
  Step 3.10:    Migrate JavaSupertypeGraph
  Step 3.11:    Migrate JavaClassFinderOverAstImpl
  Step 4.1-4.4: Cleanup (remove old code, finalize)
                Full test run, review
```

**Total: ~3-4 weeks** with testing and review cycles. The estimate is larger than the analysis document's "7-11 days" because it accounts for the careful phased approach with parallel verification.

---

*Last updated: 2026-04-20*
