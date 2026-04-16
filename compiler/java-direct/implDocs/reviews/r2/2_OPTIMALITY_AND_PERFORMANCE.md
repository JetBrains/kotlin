# Java-Direct Module: Optimality and Performance

## 1. Class Finding Strategies
The `JavaClassFinderOverAstImpl` handles file location, loading, parsing, and caching.
- **Index Building vs Eager Parsing**: The module builds a source file index but also contains eager parser fallbacks for annotations or top-level elements. Full filesystem walking and index generation could become a bottleneck for very large codebases. Leveraging incremental build indices, an external file watcher, or delaying the parse step even further (lazy parsing on-demand) could reduce initialization times.
- **Negative Lookups**: `CombinedJavaClassFinder` delegates to the AST source finder first, and then to binary finders for libraries. A class reference that does not exist in the source files will incur the cost of a full source-level fallback attempt before binary fallback. Optimizing this path—perhaps using a fast-path Bloom filter or negative lookup cache—could substantially speed up compilation.

## 2. Name Resolution Overhead
The `JavaResolutionContext` heavily relies on JLS-compliant scope resolution.
- **Callbacks and Probing**: The `resolve()` method evaluates `ClassId` instances sequentially by trying different namespaces (imports, same package, java.lang, star imports). This sequential invocation of `tryResolve: (ClassId) -> Boolean` forces the FIR layer to perform multiple lookups. Caching negative resolution hits—so that repeated lookups for the same missing `ClassId` return immediately—could minimize FIR resolution overhead.
- **Dotted-Name Fallbacks**: Resolving `a.b.C` might incur multiple internal checks (`A` as class, `A.B` as class). The probing order is inherently expensive when there are deeply nested generic packages.

## 3. Import Extraction and State Management
- **Repeated AST Traversal**: `JavaResolutionContext.extractImports(root)` processes the file AST to build a map of explicit single-type imports and a list of star imports. If contexts are re-created or spawned redundantly for different elements within the same file, this could lead to redundant AST traversals. Extracting and caching file-level metadata (imports, package name) per `JavaSyntaxNode` root would reduce allocation and traversal times.
- **String Operations**: There are significant String concatenation and suffixing actions happening during inherited nested class resolution and type conversions. Switching to a more allocation-friendly mechanism, or deferring fully qualified name string generation until strictly necessary, can improve GC pressure.
