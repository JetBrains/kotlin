# Kotlin stubs architecture

This document describes how Kotlin PSI **stubs** are modeled and built, and how the Kotlin support integrates with the IntelliJ Platform
stub infrastructure. It covers source stubs, binary (compiled) stubs, and the decompiled PSI/light-class views built on top of binaries.

> Scope note: the IntelliJ Kotlin *plugin* lives in a separate repository
> (`JetBrains/intellij-community`). This repo provides the stub interfaces, element types,
> stub builders, decompilers, and their tests.

## Overview: IntelliJ PSI stubs

- IntelliJ uses **stub trees** (a compact, serializable, indexable summary of the PSI) to support fast indexing, navigation, and partial PSI
  loading without parsing whole files. See [Stub Indexes](https://plugins.jetbrains.com/docs/intellij/stub-indexes.html) for more details.
- A stub tree mirrors the structural PSI for declarations and references but omits statement bodies and trivia. In Kotlin, only declarations
  (and selected modifiers/annotations/references)
  are stubbed.
- Each PSI element that can be stubbed has an element type. In Kotlin these extend
  `IStubElementType` (platform API) via `KtStubElementType`; an element type knows how to create PSI from an AST node or from a stub,
  serialize/deserialize the stub, and decide whether a stub should be created.
    - Note: the Kotlin code uses the platform's `IStubElementType` API. The newer platform
      `StubElementFactory`/`StubSerializer` API is not used here.
- File-level stubs implement `PsiFileStub` and are the roots consumed by indexes.

## Where things live

| Area                             | Module / package                                                        | What it contains                                                                                                                                                                                                              |
|----------------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Stub interfaces & contracts      | `compiler/psi/psi-api` → `org.jetbrains.kotlin.psi.stubs`               | `StubInterfaces.kt`, `KotlinFileStubKind.kt`, `KotlinStubVersions.kt`; element-type base `elements/KtStubElementType.java`; API registry `KtStubBasedElementTypes.kt`; base PSI `KtElementImplStub.java`                      |
| Element-type & stub impls        | `compiler/psi/psi-impl` → `…psi.stubs.elements` / `…psi.stubs.impl`     | `Kt*ElementType` impls, registry `KtStubElementTypes.java`, indexing glue `StubIndexService.kt`; stub data classes in `…psi.stubs.impl` (`KotlinPropertyStubImpl`, …), `StubUtils`, `KotlinStubOrigin`, `createConstantValue` |
| Shared cls stub builders         | `analysis/decompiled/decompiler-to-stubs` → `…analysis.decompiler.stub` | Metadata(proto)→stub builders shared by all binary formats: `ClassClsStubBuilder`, `CallableClsStubBuilder`, `TypeClsStubBuilder`, `typeAliasClsStubBuilding`, `ClsContractBuilder`, …                                        |
| JVM `.class` entry point         | `analysis/decompiled/decompiler-to-file-stubs`                          | `KotlinClsStubBuilder` (`ClsStubBuilder` for `.class`), plus the private `JvmClsAnnotationLoader`                                                                                                                             |
| Decompiled PSI / text & builtins | `analysis/decompiled/decompiler-to-psi`                                 | `KotlinClassFileDecompiler`, `KotlinDecompiledFileViewProvider`, `KtDecompiledFile`, and the built-ins stub builder `KotlinBuiltInMetadataStubBuilder`                                                                        |
| Native / KLIB                    | `analysis/decompiled/decompiler-native` → `…analysis.decompiler.konan`  | `KotlinKlibMetadataDecompiler` for `.knm` metadata (`KlibMetaFileType`)                                                                                                                                                       |
| Light classes over decompiled    | `analysis/decompiled/light-classes-for-decompiled`                      | Java-facing light classes built on decompiled declarations: `DecompiledLightClassesFactory`, `KtLightClassForDecompiledDeclaration`, `KtLightMethodForDecompiledDeclaration`, …                                               |
| Stub tests                       | `analysis/stubs`                                                        | **Generated** tests (`tests-gen`) + engines/fixtures in `testFixtures`                                                                                                                                                        |

## Core Kotlin PSI stub interfaces

Defined in `compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/stubs/StubInterfaces.kt`. All of them are implementation details,
but exposed due to the current technical limitations.

- `KotlinStubElement<T>` — base contract for all Kotlin stubs (`copyInto`, `isEquivalentTo`).

## Element types and stub creation

- `KtStubElementType<StubT, PsiT>` (`compiler/psi/psi-api/.../elements/KtStubElementType.java`) extends `IStubElementType`. It bridges AST/PSI/stub.
- Base PSI for stub-backed elements is `KtElementImplStub`.
- Element types are registered in `KtStubElementTypes` (psi-impl, the `IStubElementType`
  instances). API consumers reference element-type constants through `KtStubBasedElementTypes`
  (psi-api).
- **Serialization** — each element type writes/reads its fields via `StubOutputStream`/
  `StubInputStream` (see e.g. `KtFunctionElementType.serialize/deserialize`). Stub data classes live in
  `org.jetbrains.kotlin.psi.stubs.impl`.
- `KotlinElementTypeProvider` (`compiler/psi/psi-api/.../KotlinElementTypeProvider.kt`) is a bridge between element types and their
  implementations (`KotlinElementTypeProviderImpl`).

## Stub versions

`KotlinStubVersions` (`compiler/psi/psi-api/.../KotlinStubVersions.kt`). Bumping a version forces reindexing of the corresponding artifact
kind on the next IDE start.

- `SOURCE_STUB_VERSION` — bump on parser/PSI/stub-format changes affecting source `.kt`/`.kts`.
- `BINARY_STUB_VERSION` (private) — base for binary formats; derived constants:
    - `CLASSFILE_STUB_VERSION` — JVM `.class` stubs.
    - `BUILTIN_STUB_VERSION` — built-in metadata stubs.
    - `KLIB_STUB_VERSION` — Native/Common KLIB metadata (`.knm`).
- `JS_STUB_VERSION` is `@Deprecated` — the Kotlin/JS metadata decompiler has been removed.

## Kinds of Kotlin stubs

### 1) Source stubs

Built for `.kt`/`.kts` from the parsed PSI via `KtStubElementType` and the registered element types. Carry package, imports, declarations,
modifiers, annotations, signatures, and selected flags — but no statement bodies. Consumed by indexes (Go To, Find Usages) and by analysis
to locate declarations quickly.

### 2) Compiled / binary stubs

Built from binary artifacts without sources, by reading Kotlin metadata. All formats produce stubs implementing the same `StubInterfaces.kt`
contracts via the shared builders in
`decompiler-to-stubs` (`ClassClsStubBuilder`, `CallableClsStubBuilder`, `TypeClsStubBuilder`, …).

- **JVM `.class`** — entry point `KotlinClsStubBuilder.buildFileStub` → `doBuildFileStub`:
    1. Resolve the `KotlinJvmBinaryClass` and its `KotlinClassHeader`.
    2. If the metadata version is incompatible, return an "invalid" file stub (`createIncompatibleAbiVersionFileStub`).
    3. `createStubBuilderComponents` prepares the class/data finders and `JvmClsAnnotationLoader`.
    4. Dispatch on `header.kind`: `CLASS` → `createTopLevelClassStub`; `FILE_FACADE` /
       `MULTIFILE_CLASS_PART` → `createFileFacadeStub`; `MULTIFILE_CLASS` → `createMultifileClassStub`.
    5. The shared builders construct the tree; a `PsiFileStub<KtFile>` is returned to the platform.

    - **Annotations & constants:** `JvmClsAnnotationLoader` (a private class in
      `KotlinClsStubBuilder.kt`, not a separately named class) walks the class with
      `KotlinJvmBinaryClass` member visitors, collecting member annotations, field initializers, and annotation default values; values are
      converted via `createConstantValue`.
- **Built-ins** — `KotlinBuiltInMetadataStubBuilder` (`decompiler-to-psi`), `BUILTIN_STUB_VERSION`.
- **Native / KLIB** — `KotlinKlibMetadataDecompiler` (`decompiler-native`,
  `…analysis.decompiler.konan`), `KLIB_STUB_VERSION`, `.knm` (`KlibMetaFileType`).

### 3) Decompiled PSI and light classes

For navigation/quick-doc, the platform builds a read-only text + PSI view over binaries:

- `KotlinClassFileDecompiler` / `KotlinDecompiledFileViewProvider` / `KtDecompiledFile`
  (`decompiler-to-psi`) produce decompiled text-backed PSI.
- `light-classes-for-decompiled` builds Java light classes (`KtLightClassForDecompiledDeclaration`,
  `KtLightMethodForDecompiledDeclaration`, …) for Java interop.

The decompiled view and the binary file stubs share the same metadata deserialization, so indexes and decompiled views agree on structure
and names.

## Indexing and versioning

- Element types contribute to indexes through `indexStub`, which delegates to `StubIndexService`
  (`indexFunction`, `indexProperty`, `indexClass`, …). The Kotlin support registers file- and element-level indexes that consume
  `KotlinFileStub` and its children.
- Bump the relevant `KotlinStubVersions` constant whenever stub shape, names, or index-significant flags change.

## Key PSI integration points

- Stub-backed PSI reads children from either the stub or the AST. Example:
  `KtClassOrObject.getSuperTypeList()` uses `getStubOrPsiChild(SUPER_TYPE_LIST)`.
- `KtClassOrObject.isLocal()` / `getClassId()` are stub-aware: the stub carries the data; PSI falls back to computation (and resets
  `isLocal` on `subtreeChanged`).

## Testing

- Stub behavior is covered by **generated** tests in `analysis/stubs` (`tests-gen`), driven by the engines/fixtures in
  `analysis/stubs/testFixtures/org/jetbrains/kotlin/analysis/stubs`:
  `AbstractStubsTest`, `AbstractSourceStubsTest`, `AbstractCompiledStubsTest`, `StubsTestEngine`,
  `SourceStubsTestEngine`, `CompiledStubsTestEngine` (plus `additionalStubInfoExtractor`,
  `TestGenerator`).
- Regenerate the generated tests with `./gradlew generateTests` after adding test data.

## Useful entry points

- `compiler/psi/psi-api/.../psi/stubs/StubInterfaces.kt` — all Kotlin stub contracts.
- `compiler/psi/psi-api/.../psi/stubs/KotlinFileStubKind.kt` — file stub classification.
- `compiler/psi/psi-api/.../psi/stubs/KotlinStubVersions.kt` — versioning.
- `compiler/psi/psi-api/.../psi/stubs/elements/KtStubElementType.java` — AST/PSI/stub bridge.
- `compiler/psi/psi-impl/.../psi/stubs/elements/` — element-type impls, `KtStubElementTypes`,
  `StubIndexService`; stub data classes in `…/psi/stubs/impl/`.
- `analysis/decompiled/decompiler-to-stubs/...` — shared cls builders (`ClassClsStubBuilder`, `CallableClsStubBuilder`,
  `TypeClsStubBuilder`).
- `analysis/decompiled/decompiler-to-file-stubs/.../KotlinClsStubBuilder.kt` — JVM `.class` entry.
- `analysis/decompiled/decompiler-to-psi/...` — decompiled PSI + `KotlinBuiltInMetadataStubBuilder`.
- `analysis/decompiled/decompiler-native/.../konan/KotlinKlibMetadataDecompiler.kt` — KLIB/Native.

## FAQ

- **Why both binary file stubs and decompiled PSI?** Stubs power indexing and are lightweight for headless operations; decompiled PSI/light
  classes give a readable, navigable, Java-interoperable view. They share metadata so they stay consistent.
- **Which stub version do I bump?** `SOURCE_STUB_VERSION` for source `.kt` changes;
  `CLASSFILE_STUB_VERSION` for JVM `.class` builders; `BUILTIN_STUB_VERSION` for built-ins;
  `KLIB_STUB_VERSION` for Native/KLIB. (JS no longer produces stubs.)
- **Where do I start debugging a missing stub element?** Source: the element's `Kt*ElementType`
  and its `shouldCreateStub`. Binary (JVM): `KotlinClsStubBuilder.doBuildFileStub`, then into the shared `ClassClsStubBuilder` /
  `CallableClsStubBuilder` in `decompiler-to-stubs`. Compare against the generated stub tests in `analysis/stubs`.
