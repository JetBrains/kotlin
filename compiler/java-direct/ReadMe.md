# Java Facade for Kotlin compiler

A lightweight "direct" implementation of the compiler's Java model, replacing the PSI-based
facade that pulls a lot of IntelliJ platform code into the compiler. Enabled by the
`-Xjava-direct` compiler option, which switches both the Java source and the binary class
paths to this module.

## Purpose

Kotlin has bidirectional Java interop: a module can contain Java files referencing Kotlin
declarations and vice versa, so the compiler cannot rely on Java files being available in
binary form and must process Java sources directly, exposing their declarations to FIR
resolution. This was originally implemented via IntelliJ platform infrastructure (the
PSI-based Java facade); this module provides the same `JavaClassFinder` /
`org.jetbrains.kotlin.load.java.structure` model without it.

## Architecture

- **Parser**: the lightweight KMP Java parser (`org.jetbrains:syntax-api`,
  `org.jetbrains:java-syntax`, published by the Fleet team), producing a light-tree
  structure with no IntelliJ platform dependencies.
- **Laziness**: source roots are scanned lazily — directory roots are descended only when a
  package is requested; files are pre-scanned (lexer-only) for package/top-level names and
  parsed only when FIR asks for a class. Model extraction on top of the parsed tree is also
  lazy and cached.
- **Java model**: `JavaClassFinderOverAstImpl` (sources) and
  `JavaClassFinderOverBinaryIndex` (binaries) return model entities from
  `org.jetbrains.kotlin.java.direct.model`.
- **Resolution**: unlike the PSI facade, all non-Java-source references (binary classes,
  Kotlin declarations) are resolved through FIR via the `FirSession`, and the results are
  wrapped back into the Java model (`FirBackedJava*` classes).

Detailed maps live in `implDocs/`:
- `implDocs/ARCHITECTURE.md` — key files, callback patterns, JLS implicit rules.
- `implDocs/RESOLUTION_PIPELINE.md` and `implDocs/RESOLUTION_SCHEMA.md` — the resolution
  entities and the end-to-end scenarios (classifier lookup, JLS 6.4/6.5 name resolution,
  inherited member types, implicit outer type arguments, annotations, constants).
- `implDocs/PSI_FREE_ROADMAP.md` — the PSI-free / platform-free status and remaining work.

## Tests

The module contains unit tests (`JavaParsing*`) and also "steals" all phased diagnostics
and box tests that contain Java files from the main compiler test data
(`JavaUsingAstPhasedTestGenerated`, `JavaUsingAstBoxTestGenerated`), plus a
java-direct-owned `testData/diagnostics` root for javac-conformant behaviour that
deliberately differs from the PSI model.
