Kotlin stubs architecture in IntelliJ IDEA

This document describes how Kotlin stubs are modeled and built in the IntelliJ Platform and how the Kotlin plugin integrates with the general stub infrastructure. It also outlines how source, compiled, and decompiled stubs are produced and consumed by analysis.

Overview: IntelliJ PSI stubs
- IntelliJ uses stub trees (a compact, indexable AST) to support fast indexing, navigation, and partial PSI loading without parsing the whole file.
- A stub tree mirrors the structural PSI for declarations and references but omits bodies and trivia. In Kotlin, only declarations and some modifiers/annotations are stubbed.
- Each PSI element that can be stubbed has an element type derived from `IStubElementType`. Element types know how to create PSI from stubs and decide whether a stub should be created.
- File-level stubs implement `PsiFileStub` and are the roots used by indexes.

Core Kotlin PSI stub types
- Interfaces for all Kotlin stubs live in compiler/psi/psi-api under org.jetbrains.kotlin.psi.stubs (see `StubInterfaces.kt`):
  - `KotlinStubElement<T>` and `KotlinPlaceHolderStub<T>`: base contracts for Kotlin stub elements.
  - `KotlinFileStub`: root stub for Kotlin files. It exposes kind and package name, and allows searching imports by alias.
  - `KotlinStubWithFqName`, `KotlinClassifierStub`, `KotlinClassOrObjectStub`, `KotlinTypeAliasStub`, etc.: typed contracts for named declarations.
  - `KotlinDeclarationWithBodyStub`: marks declarations that may have a body and records whether they actually have one.
- File stub kinds are described by `KotlinFileStubKind` (`compiler/psi/psi-api/.../KotlinFileStubKind.kt`):
  - `WithPackage.File`: a regular Kotlin source file.
  - `WithPackage.Script`: a script file.
  - `WithPackage.Facade.Simple` and `WithPackage.Facade.MultifileClass`: file facades for top-level declarations compiled to classes (including multifile facades).
  - `Invalid`: used when metadata is corrupted or incompatible; carries a human-readable error message used in the decompiled view.
- Stub element types are implemented via `KtStubElementType` (`compiler/psi/psi-api/.../elements/KtStubElementType.java`):
  - Bridges between an AST/green tree and PSI/stub-based elements, provides createPsi from AST or Stub, `externalId`, and `shouldCreateStub` logic.
  - Only declarations that are index-relevant are stubbed. Notably: `KtClassOrObject` and `KtFunction` always get stubs; `KtProperty` is stubbed when not local; otherwise it defers to the parent’s decision.
- Stub versions are tracked in `KotlinStubVersions` (`compiler/psi/psi-api/.../KotlinStubVersions.kt`):
  - `SOURCE_STUB_VERSION`: bump when source PSI structure or stub format changes.
  - `BINARY_STUB_VERSION` and derivatives: `CLASSFILE_STUB_VERSION`, `BUILTIN_STUB_VERSION`, `JS_STUB_VERSION`, `KLIB_STUB_VERSION` for different binary formats. Changing these triggers reindexing of affected files.

Kinds of Kotlin stubs:
1) Source stubs
   - Built for .kt and .kts files directly from the parsed PSI using `KtStubElementType` and the Kotlin parser.
   - Contain package, imports, declarations, modifiers, annotations, signatures, and select flags but no statement bodies.
   - Used by indexes for Navigate To, Find Usages, and by analysis frontends to quickly locate declarations.

2) Compiled/binary stubs (classfiles, built-ins, JS/metadata, KLIB)
   - Built from binary artifacts without source by reading Kotlin metadata and/or platform-specific binary info.
   - Shared conventions and versions are guided by `KotlinStubVersions`’ binary-related constants.
   - On the JVM:
     - Classfile stubs are constructed from .class files containing Kotlin metadata (and from synthetic classes representing facades/multifile facades).
     - See `analysis/decompiled/decompiler-to-file-stubs/src/.../KotlinClsStubBuilder.kt` for entrypoint, which decides whether to produce a `PsiFileStub<KtFile>` and orchestrates deserialization.
     - Supporting builders: `ClassClsStubBuilder`, `CallableClsStubBuilder`, `TypeClsStubBuilder`, and helpers in the same package build classifier/member/type stubs from proto and signature data.
     - `AnnotationLoaderForClassFileStubBuilder` collects annotations and constant initializers from the binary via `KotlinJvmBinaryClass` visitors.
   - Built-ins and other binary container kinds follow analogous pipelines with their own versions (e.g., `BUILTIN_STUB_VERSION`).
   - JS and KLIB formats are supported by their respective subsystems; their stubs still implement the same Kotlin stub interfaces and flow into IntelliJ indexing in the same way.

3) Decompiled PSI (read-only PSI built for binaries)
   - For navigation and quick documentation, IntelliJ can construct a read-only PSI view (decompiled text) on top of binaries.
   - The decompiler-to-psi module contains components like:
     - `KotlinClassFileDecompiler` (`analysis/decompiled/decompiler-to-psi/.../KotlinClassFileDecompiler.kt`): provides a decompiled view for Kotlin-aware classfiles.
     - `KotlinDecompiledFileViewProvider` (`analysis/decompiled/decompiler-to-psi/.../KotlinDecompiledFileViewProvider.kt`): supplies PSI for decompiled files, backed by text produced from metadata.
   - This PSI is consistent with and complementary to the file stubs produced by decompiler-to-file-stubs; the two systems share metadata deserialization logic so indexes and decompiled views agree on structure and names.

How stub building works (IntelliJ + Kotlin plugin):
- IntelliJ’s indexing framework asks language plugins to provide ClsStubBuilder for binaries and relies on PSI/stub element types for source.
- Source path:
  1. Kotlin file is parsed into PSI.
  2. `KtStubElementType.shouldCreateStub` determines which PSI elements get stubbed.
  3. Stub elements are created and attached beneath the `KotlinFileStub`; indexes consume them.
- Binary path (JVM example):
  1. For a `VirtualFile` (.class), `KotlinClsStubBuilder.buildFileStub` is invoked.
  2. `doBuildFileStub` reads file content and metadata, identifies the `KotlinFileStubKind` (facade vs part vs simple file representation).
  3. `createStubBuilderComponents` prepares `NameResolver`/`TypeTable` and deserializers.
  4. Class/Callable/Type builders construct the tree implementing StubInterfaces.kt contracts.
  5. The resulting `PsiFileStub<KtFile>` is returned to IntelliJ for indexing and light PSI creation.
- Annotation and constants loading:
  - `AnnotationLoaderForClassFileStubBuilder` uses `KotlinJvmBinaryClass` visitors to record annotations and constant initializers; values are converted via `createConstantValue` and attached to stubs where relevant.

Key Kotlin PSI integration points
- PSI classes like `KtClassOrObject` provide stub-based implementations and are created from either AST or Stub via `KtStubElementType` reflection bridges. Example method: `getSuperTypeList()` uses `getStubOrPsiChild` to efficiently access children from either source PSI or stub.
- `KtClassOrObject.isLocal()` and `getClassId()` are stub-aware: the stub carries locality and `ClassId`; when missing, PSI falls back to computation.

File stub kinds and facades
- `KotlinFileStub.kind` encodes whether a file is a regular file/script, a facade (`Simple` or `MultifileClass`), or invalid.
- Facades represent the class-level view of top-level declarations compiled from a file or a set of files; there are helpers for the simple part names of multifile facades.

Indices and versioning
- The Kotlin plugin contributes file- and element-level indexes that consume `KotlinFileStub` and its children.
- Bumping a stub version in `KotlinStubVersions` forces reindexing of the corresponding artifact kind at the next IDE startup. This is required if stub shapes, names, or index-significant flags change.

Testing and fixtures
- `analysis/stubs/testFixtures` and tests(-gen) contain harnesses and generated tests operating on produced stubs.
- `analysis/decompiled/.../testData` holds golden files for decompiled text and stub shapes; for example, `decompiler-to-psi/testData/builtins/*.stubs.txt` and `decompiler-to-file-stubs/testData/clsFileStubBuilder/*` verify binary stub structure.
- `Abstract*Test classes` in `analysis/decompiled/decompiler-to-file-stubs/testFixtures` exercise `KotlinClsStubBuilder` and friends.

Useful entry points in this repo
- `compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/stubs/StubInterfaces.kt`: contracts for all Kotlin stubs.
- `compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/stubs/KotlinFileStubKind.kt`: file stub classification.
- `compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/stubs/KotlinStubVersions.kt`: versioning for source/binary stubs.
- `compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/stubs/elements/KtStubElementType.java`: element type glue between AST and stubs.
- `analysis/decompiled/decompiler-to-file-stubs/src/.../KotlinClsStubBuilder.kt`: JVM classfile stub builder entry.
- `analysis/decompiled/decompiler-to-psi/src/.../KotlinClassFileDecompiler.kt` and `KotlinDecompiledFileViewProvider.kt`: decompiled PSI pipeline.
- `analysis/decompiled/decompiler-to-stubs/src/...`: per-declaration builders (`ClassClsStubBuilder`, `CallableClsStubBuilder`, `TypeClsStubBuilder`).

Notes from commit history
- Many implementation notes and rationale can be found in commits prefixed with [stubs] or [decompiler] in this repository’s history. They often describe shape changes, version bumps, or design choices in how metadata is converted to stubs or decompiled text. When making changes to stub structure or binary parsing, search these commits and consider bumping the appropriate version constants.

FAQ
- Why do we need both binary file stubs and decompiled PSI? Stubs power indexing and are lightweight for headless operations; decompiled PSI provides a readable, navigable source-like view for users. They share the same metadata so they stay consistent.
- When should I bump `SOURCE_STUB_VERSION` vs `CLASSFILE_STUB_VERSION`? Bump source when parser/psi/stub impl changes affect source .kt stubs. Bump classfile for changes in the classfile stub builders. For built-ins/JS/KLIB, use their dedicated version constants.
- Where do I start debugging a missing element in stubs? For source: check KtStubElementType.shouldCreateStub and the specific element type. For classfiles: start at `KotlinClsStubBuilder.doBuildFileStub`, then follow into `ClassClsStubBuilder`/`CallableClsStubBuilder`. Inspect testData golden files to see expected shapes.
