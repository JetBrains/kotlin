# Low Level API

Low-level API (LL API for short) is an interlayer between Analysis API FIR Implementation and FIR compiler. Low-level API is responsible for:

* Finding corresponding `FirElement` by `KtElement`
* Lazy resolving of `FirDeclaration`
* Collecting diagnostics for `FirDeclaration`
* Incremental code analysis
* Implementing FIR providers using IntelliJ indexes

You can read about how FIR compiler works [here](../../fir/fir-basics.md).

The entry point for LL API is `FirModuleResolveState`.` FirModuleResolveState` is a view of project modules visible from the current module.
The lifetime of
`FirModuleResolveState` is limited by Kotlin Out of Block Modification.

## Mapping KtElement to FirElement (KT -> FIR) & Incremental Analysis

To implement some stuff in Analysis API, we need to get resolved `FirElement` by the given `KtElement`. Also, this functionality is
considered to be used not very often (mostly, for the files opened in the editor) as it resolves declarations to the `BODY_RESOLVE` and
collects `KT -> FIR` mappings for it.

Finding `KT -> FIR` mappings works like the following:

* For every `KtFile` we need mapping for, we have `FileStructure` which contains a tree like-structure of `FileStructureElement`
* `FileStructureElement` can be one of the following:
    * `ReanalyzableStructureElement` represents a non-local declaration that can be incrementally reanalyzed after non-out of block change
      inside it.
    * `NonReanalyzableDeclarationStructureElement` represents non-local declaration which can not be incrementally reanalyzed
    * `RootFileStructureElement` represents file except all declarations inside
* `FileStructureElement` form a nested tree-like structure
* Then we want to get `KT -> FIR` mapping we find containing `FileStructureElement`. If is not up-to-date we rebuild it and then take from
  it.

The following declarations can be reanalyzed (in other words, can be represented as ReanalyzableStructureElement):

* Functions with explicit return type
* Properties with explicit type
* Secondary constructors
* Getters/setters of the property with explicit type

# Lazy Declaration resolving

FIR in compiler mode works by sequentially running every resolve phase on all files at once like shown in pseudo-code:

```kotlin
for (phase in allResolvePhases) {
    for (file in allFirFiles) {
        runPhaseOnFile(file, phase)
    }
}
```

Such behavior does not work for the Analysis API. Analysis API needs to resolve one specific declaration to the minimum possible phase. To
solve that problem, there are *lazy phases*. Lazy phases can be run only on a single declaration, not on the whole `FirFile`.

Suppose we need to resolve some `FirDeclaration` to phase number `N`:

* First, we resolve containing `FirFile` to the `IMPORTS` phase,
* Then we resolve file annotations,
* When we find a non-local container for our declaration as only non-local declarations can be lazily resolved for now,
* Finally, resolve *only* that container from phase number `1` to phase `N`

All resolve happens under containing file-based *write lock*.

## Project Module Structure

The `FirModuleResolveState` represents a view from a specific module (**root module**) to the dependent modules. A module is represented by:

* `FirIdeSourcesSession` the implementation of `FirSession` (FIR compiler representation of a module)
* `ModuleFileCache` the `KtFile -> FirFile` cache & also caches for FIR providers
