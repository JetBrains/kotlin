# Low-Level API FIR

Low-level API FIR (LL API or LL FIR for short) is an interlayer between Analysis API FIR Implementation and FIR compiler.
Low-level API FIR is responsible but not limited for:
- Finding corresponding [FirElement](../../../compiler/fir/tree/gen/org/jetbrains/kotlin/fir/FirElement.kt) by `KtElement`
- Lazy resolution of declarations ([FirElementWithResolveState](../../../compiler/fir/tree/gen/org/jetbrains/kotlin/fir/FirElementWithResolveState.kt))
- Collecting diagnostics for [FirDeclaration](../../../compiler/fir/tree/gen/org/jetbrains/kotlin/fir/declarations/FirDeclaration.kt)
- Incremental code analysis
- Implementing FIR providers using the Analysis API implementor's declaration/package/etc. providers (e.g., IntelliJ indexes in the IDE)

You can read about how FIR compiler works [here](../../fir/fir-basics.md).

The entry point for LL API is [LLResolutionFacade](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/api/LLResolutionFacade.kt).
`LLResolutionFacade` represents a project view from a use-site [KaModule](../../../analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/projectStructure/KaModule.kt).
The lifetime of `LLResolutionFacade` is limited by modification events.

[LowLevelFirApiFacade](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/api/LowLevelFirApiFacade.kt)
file contains a useful API surface to interact with Low Level API FIR from Analysis API FIR. 

## Documentation

There are a bunch of different areas.
Docs for each area are sorted by context depth – from the basic overview to the deepest implementation details.
- Mapping from `KtElement` to `FirElement` (*KT -> FIR*) & Incremental Analysis & Collecting diagnostics
  1. [FirElementBuilder](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/element/builder/FirElementBuilder.kt)
     is responsible for mapping from `KtElement` to `FirElement`.
  2. [FileStructure](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/file/structure/FileStructure.kt)
     is a tree like-structure of `FileStructureElement` which is associated with some `KtFile`.
     Aggregates information about *KT -> FIR* mapping and diagnostics for associated file.
  3. [FileStructureElement](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/file/structure/FileStructureElement.kt)
     is a representation of specific `KtElement`.
     Is responsible for *KT -> FIR* mapping and diagnostics for the specific `KtElement`.
  4. [FileStructureElementDiagnosticRetriever](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/diagnostics/FileStructureElementDiagnosticRetriever.kt)
     is responsible to collect diagnostics for `FileStructureElement`.
  5. [LLFirDeclarationModificationService](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/file/structure/LLFirDeclarationModificationService.kt)
     is a service which is responsible for `FileStructure` invalidation in the case of associated PSI modification.
- Lazy resolution
  1. [FirResolvePhase](../../../compiler/fir/tree/src/org/jetbrains/kotlin/fir/declarations/FirResolvePhase.kt)
     to understand what the compiler phases are and what is the basic difference between the CLI and the Analysis API modes.
  2. [LLFirModuleLazyDeclarationResolver](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/lazy/resolve/LLFirModuleLazyDeclarationResolver.kt)
     is the entry point for lazy resolution.
     Receives some `FirElementWithResolveState` element and a desired phase and resolve this element to this phase.
  3. [LLFirResolveDesignationCollector](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/lazy/resolve/LLFirResolveDesignationCollector.kt)
     is a designation collector.
     Collects `LLFirResolveTarget` for the specific `FirElementWithResolveState`.
     Decides which element can be resolved lazily and which cannot.
  4. [LLFirResolveTarget](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/api/targets/LLFirResolveTarget.kt)
     is an instruction on how to resolve specific `FirElementWithResolveState`.
  5. [LLFirLazyResolverRunner](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/transformers/LLFirLazyResolverRunner.kt)
     is responsible
     for running [LLFirLazyResolver](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/transformers/LLFirLazyResolver.kt)
     for the specific phase and making sure that it worked correctly.
  6. [LLFirTargetResolver](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/transformers/LLFirTargetResolver.kt)
     is the core part of lazy resolution.
     We have a separate implementation of `LLFirTargetResolver` for each compiler phase,
     each of which is responsible for all the resolution logic for the associated phase.
  7. [LLFirLockProvider](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/file/builder/LLFirLockProvider.kt)
     is responsible for locking logic which is widely used by `LLFirTargetResolver` during resolution.
- [ContextCollector](../../../analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/util/ContextCollector.kt)
  represents resolution context of a specific place in code (a context)

## Project Module Structure

The `LLResolutionFacade` represents a view from a specific module (**root module**) to the dependent modules. A module is represented by:
* `LLFirSession` – the implementation of `FirSession` (FIR compiler representation of a module)
* `ModuleFileCache` – the `KtFile -> FirFile` cache & also caches for FIR providers
