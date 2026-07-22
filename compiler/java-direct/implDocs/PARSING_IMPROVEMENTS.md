# Parsing Pipeline: Potential Improvements

**Date**: 2026-04-23
**Status**: Analysis complete, no implementation planned yet.

---

## Current Pipeline

```
Source (CharSequence)
    │
    ▼
Lexer (JavaSyntaxDefinition.createLexer)
    → TokenList: lexStarts: IntArray, lexTypes: Array<SyntaxElementType>
    │
    ▼
Parser (JavaParser.fileParser.parse)
    → SyntaxTreeBuilder accumulates START/DONE/ERROR markers into MarkerPool + IntArrayList
    │
    ▼
prepareProduction(builder)
    → ProductionResult: materialised ProductionMarkerList snapshot + TokenList reference
    │
    ▼
buildJavaLightTree (2 passes over the snapshot)
    Pass 1: composite + token index computation (parent, done-index, type, offsets, token-parent)
    Pass 2: children list construction
    │
    ▼
JavaLightTree (precomputed IntArray-backed lookups, O(1) access)
```

## Completed: Pass Merge (1+2 → 1)

The original implementation had three post-parse passes. Passes 1 (composite indices) and 2
(token-to-parent assignment) shared the same traversal order and stack structure over
`ProductionMarkerList`. They were merged into a single `buildCompositeAndTokenIndices` function,
eliminating one full marker scan and one stack allocation.

Pass 3 (children construction) has a different access pattern (per-composite walks with jumps
via `doneForStart`) and remains separate. It could theoretically be folded into the merged pass
by maintaining per-open-composite child accumulators, but the readability cost outweighs the
marginal constant-factor win — the children pass is already O(n) in total markers.

## Potential: Eliminate Post-Parse Passes Entirely

### Idea

The parser's `mark()` / `done(type)` / `advance()` / `error()` calls happen in exactly the
order that the merged pass walks the production markers. If we could intercept these calls
during parsing, we could build all `JavaLightTree` arrays as a side effect:

- **`mark()`** → push to stack, record `parentStartIndex[i] = peekOrRoot()`
- **`advance()`** → assign `tokenParentStart[t] = top()`
- **`done(type)`** → assign trailing tokens, pop stack, record `doneForStart`, type, end offset;
  finalize the closed composite's children list
- **`error()`** → record error flag, type, offsets

This would collapse the pipeline from "parse → materialise → 2 passes" to "parse (with hooks)
→ JavaLightTree", skipping:
- The `ProductionMarkerList` materialisation (marker pool, `IntArrayList`, flyweight lookups)
- Both post-parse marker scans
- The `prepareProduction()` whitespace-balancing step (we already exclude whitespace from
  children during construction)

### Why It's Not Possible Today

Three API constraints in `com.intellij.platform.syntax.parser` (syntax-api v0.3.374) block this:

1. **`@ApiStatus.NonExtendable`** on `SyntaxTreeBuilder` — custom implementations that intercept
   `mark()`/`done()`/`advance()` are not supported. `SyntaxTreeBuilderFactory` only returns
   `SyntaxTreeBuilderImpl`.

2. **No marker event callbacks** — the only hooks are `WhitespaceSkippedCallback` (fires on
   whitespace/comment skip) and `SyntaxElementTypeRemapper` (token type remapping). There is no
   `onMarkerOpened` / `onMarkerClosed` / `onTokenAdvanced` listener.

3. **`prepareProduction()` is the sole extraction API** — it internally casts to
   `SyntaxTreeBuilderImpl`, so marker data can only be consumed through the materialised
   `ProductionResult` snapshot.

### What an Upstream Change Would Look Like

Either of these additions to the syntax-api would unblock the optimisation:

**Option A — Parsing event listener** (preferred, minimal API surface):
```kotlin
// New callback interface
fun interface ParsingEventListener {
    fun onMarkerDone(markerIndex: Int, type: SyntaxElementType, startTokenIndex: Int, endTokenIndex: Int)
    fun onMarkerError(markerIndex: Int, startTokenIndex: Int, endTokenIndex: Int)
    fun onTokenAdvanced(tokenIndex: Int)
}

// New factory method
SyntaxTreeBuilderFactory.Builder.withParsingEventListener(listener: ParsingEventListener): Builder
```

This fires events during parsing without exposing `SyntaxTreeBuilderImpl` internals. The
listener receives enough data (marker index, type, token range) to build all `JavaLightTree`
arrays incrementally. `prepareProduction()` becomes unnecessary for this consumer.

**Option B — Make `SyntaxTreeBuilder` extendable** (more invasive):
Remove `@ApiStatus.NonExtendable`, allowing a delegating wrapper that intercepts calls.
Less likely to be accepted upstream — it exposes the full mutable API surface.

### Expected Impact

For java-direct's workload (parsing thousands of Java files per compilation), eliminating the
intermediate materialisation would:
- Remove all `MarkerPool` object reuse / flyweight lookup overhead
- Remove two O(n) array scans over production markers
- Remove the `IntArrayList` → `IntArray` snapshot copy
- Allow `JavaLightTree` construction to overlap with parsing (no sequential dependency)

The practical benefit depends on the ratio of parse time to post-parse construction time.
Measurements would be needed to quantify the gain, but the architectural simplification alone
(fewer intermediate representations) is valuable.
