## Scope of this review

This document focuses on runtime and structural efficiency of the current java-direct implementation, especially in:

- file indexing and parsing
- name resolution
- repeated AST traversal
- cross-file supertype and inherited-inner-class support

## Main strengths

### 1. The source/binary split avoids unnecessary work

`CombinedJavaClassFinder.kt` is an effective first-level optimization.

- It avoids paying java-direct costs for classes that are clearly not in the source index.
- It keeps library/JDK access on the existing binary path.

That is a strong architectural optimization because it reduces the amount of new logic executed on the hot path.

### 2. Indexing strategy is pragmatic and likely beneficial in real projects

`JavaClassFinderOverAstImpl.kt` uses two indexing modes:

- full parse for small files
- lightweight line scanning for large files

This is a smart trade-off. It recognizes that eagerly parsing every Java source file is wasteful, but also that repeatedly reparsing small files later is unnecessary.

The small-file path also eagerly caches all top-level classes in a file, which is a good locality optimization.

### 3. Lazy properties are used consistently across the model layer

The AST-backed model classes rely heavily on lazy computation. That is appropriate because FIR may inspect only part of the Java surface.

Examples include:

- `JavaClassOverAst.memberResolutionContext`
- `JavaClassOverAst.fqName`
- `JavaClassOverAst.typeParameters`
- `JavaClassifierTypeOverAst.rawTypeName`
- many field/method/annotation-derived values

This strongly supports the module’s “pay only for what is actually used” design goal.

### 4. Some local caching is well targeted

Particularly useful caches include:

- `classCache` in `JavaClassFinderOverAstImpl`
- `supertypeCache`
- `inheritedInnerClassesCache`
- per-invocation `tryResolve` cache inside `JavaResolutionContext.resolve(...)`
- `findLocalClassCache` in `JavaResolutionContext`

These caches address genuine repeated-work patterns, especially in recursive lookup paths.

## Main performance concerns

### 1. Full-file parsing is still repeated more often than ideal

Despite the caches, `JavaClassFinderOverAstImpl` still reparses files in several situations:

- `parseTopLevelClassFromFile(...)` reparses the file when the top-level class is not already cached
- package annotations use a separate parse path in `indexPackageInfo(...)`
- supertype extraction may fall back to AST/class access paths that eventually depend on parsed classes

The implementation is already careful, but the system still appears parse-heavy relative to the amount of information often needed.

### 2. The lightweight index is intentionally incomplete, which shifts cost downstream

The lightweight path only extracts package and top-level names. That keeps indexing fast, but it means later operations may need full parsing even for modest metadata queries.

Examples of metadata that are currently not cheaply available from the index:

- direct supertype names
- nested type declarations
- annotation presence
- canonical-vs-secondary class metadata beyond filename matching

This is a reasonable first version, but it creates downstream work in resolution-heavy scenarios.

### 3. `JavaResolutionContext` likely does too much work on hot paths

`JavaResolutionContext.kt` is the main candidate for both CPU cost and accidental algorithmic inefficiency.

Potentially expensive patterns include:

- repeated string splitting and joining for dotted names
- recursive prefix resolution in `resolveNestedClassToClassId(...)`
- repeated outer-class-chain walking
- repeated map/set merging for inherited inner classes
- multiple fallback probes for a single unresolved reference

The code adds local caches to offset this, which helps, but the control flow still suggests a relatively expensive resolution path compared with a more normalized internal representation.

### 4. AST traversal is often ad hoc rather than normalized once

Several model classes repeatedly inspect `node.children`, `findChildByType(...)`, and `getChildrenByType(...)` in local lazy properties and helper functions.

That keeps code straightforward, but it can be suboptimal when the same node is queried many times for:

- modifiers
- identifiers
- type nodes
- annotations
- sibling relationships

The module has not yet fully chosen between two strategies:

- very cheap raw AST access everywhere
- or a normalized intermediate representation for hot node shapes

Currently it sits in the middle, which risks incurring the overhead of both approaches.

### 5. Ambiguity and inherited-inner-class support may become a scalability hotspot

The cross-file support added around direct supertypes and inherited inner classes is functionally valuable, but it increases lookup cost.

In particular, `collectInheritedInnerClasses(...)` and the aggregated maps used by `JavaResolutionContext` can become expensive when:

- class hierarchies are deep
- many nested classes exist
- resolution probes repeatedly revisit the same chain

The current caching helps, but this area looks like the most likely place for cost growth on larger codebases.

## Most promising optimization directions

### 1. Introduce a richer lightweight index

Best improvement per complexity, in my view.

Extend the lightweight index to capture selected metadata during initial scanning or a cheap partial parse, for example:

- direct supertype textual names
- presence and names of top-level nested declarations
- package-info/package annotation markers
- possibly import summaries

That would let the class finder answer more questions without escalating to full parsing.

### 2. Split resolution into smaller precomputed components

`JavaResolutionContext` currently combines storage, policy, recursion, and cache management.

Performance could improve if resolution precomputed and reused:

- a normalized lookup object for simple-name priority
- a normalized representation of dotted-name segments
- per-containing-class inherited-inner-class lookup tables

This would likely improve both speed and maintainability.

### 3. Reduce repeated AST shape queries for hot node types

For high-traffic nodes such as classes, fields, and type references, it may be worth caching frequently used child slots:

- identifier
- modifier list
- type node
- extends/implements lists

Even a small internal structural memoization layer could reduce repeated tree walking.

### 4. Consider parse-result caching at file granularity

Today the class cache caches adapted `JavaClass` objects, but the parsed syntax tree itself does not appear to be reused as a first-class cache entry across all operations.

A bounded file-AST cache could help when the same file is touched for:

- top-level class lookup
- package annotation indexing
- direct supertype analysis
- sibling class access

The trade-off is memory consumption, so this would need measurement.

### 5. Measure the inherited-inner-class path explicitly

The cross-file supertype/nested-class logic is sophisticated enough that it deserves dedicated profiling.

If the module is to move from “mostly correct” to “production-ready and efficient”, this area should be benchmarked separately.

## Overall optimality assessment

The current implementation is reasonably optimized for a feature-complete prototype or an advanced in-progress subsystem. The major architectural choices are good, and the code already shows awareness of hot paths.

However, the implementation does not yet look fully optimal for a long-term production path. The biggest reason is not one isolated inefficiency, but rather the combination of:

- repeated parsing/AST inspection
- a very large resolution engine
- layered fallback logic for cross-file semantics

In short: the module is already pragmatic and performance-conscious, but there is still meaningful headroom. The most valuable improvements would likely come from richer indexing and a more explicitly staged resolution pipeline, not from micro-optimizing individual functions.