# Architecture of Content Scopes & Resolution Scopes

**Last update:** January 28, 2025

**Issue:** [KT-73290](https://youtrack.jetbrains.com/issue/KT-73290) Analysis API: Improve the architecture of content scopes and resolution scopes

This document is an **implementation design document** for [KT-73290](https://youtrack.jetbrains.com/issue/KT-73290/Analysis-API-Improve-the-architecture-of-content-scopes-and-resolution-scopes). Its aim is to outline the technical architecture that should be implemented to improve the consistency of content scopes and resolution scopes.

## Overview

Summary of general ideas (elaborated on in the sections below):

* We distinguish between `KaModule` *content scopes* and *base content scopes*. The former is the module’s full content scope with all enlargements and restrictions, while the latter is the scope defined by the platform which derives from the project structure.  
* Analysis API platforms and the Analysis API implementation have the ability to enlarge and restrict content scopes with custom `KotlinContentScopeRefiner`s.  
* Content scopes are computed from base content scopes using `KaContentScopeProvider`. This service becomes the default implementation of `KaModule.contentScope`.  
* Resolution scopes are computed from a module’s content scope and its dependencies’ content scopes. The responsible service `KaResolutionScopeProvider` uses a fixed algorithm and is implemented on the Analysis API side.

## Base content scopes & content scopes

A `KaModule` has two kinds of scopes:

* **Base content scope:** The scope which covers the module’s content according to the project structure. This scope needs to be provided by platform implementations.  
* **Content scope:** The scope which covers the module’s content taking run-time extensions into account. For example, resolve extensions (one example of a run-time extension) may adjust the content scope to shadow some files and include other files. This scope is computed by the Analysis API implementation from the base content scope and additional refinements.

The **reason for this split** derives from architectural problems associated with only having content scopes:

* Content scope refinement depends on the `KaModule` as we need an anchor point for the calculation. This means that content scope calculation would need to be lazy (as we need to construct the `KaModule` before we can get the content scope), which is an additional implementation burden placed on the Analysis API platform. It also makes the `KaModule` API more messy by leaking implementation considerations into the documentation.  
  * In particular, resolve extensions lead to a chicken and egg problem. To construct the full content scope of a `KaModule`, we need to calculate the shadowed scopes and whether a module has resolve extensions, but we also need to have the `KaModule` in hand to call `KaResolveExtensionProvider.provideExtensionsFor`. This requires laziness.  
* As a practical consideration, `KaModule` implementations would have to repeat calls to the engine service which refines content scopes. In essence, each `KaModule.contentScope` implementation would look like this: `lazy { refineContentScope(baseContentScope) }`. At that point, it’s easier to simply formalize the concept of a “base content scope.”

As for the **implementation**, we make the following changes to `KaModule`:

* Introduce a property `baseContentScope`.  
* Provide a default implementation for `contentScope` which delegates to `KaContentScopeProvider` (introduced further below).

## Content scope refiners

Content scopes can be *enlarged* and *restricted* by `KotlinContentScopeRefiner`s. They allow specifying additional scopes which are either applied as a union or an intersection to the original content scope. They are platform components registered via an extension point.

The enlargement (union) scopes are applied first, and in a second step the restriction (intersection) scopes, regardless of the order of registered `KotlinContentScopeRefiner`s. This ensures that the restriction scopes can also cover any files which are added via enlargement scopes.

Content scopes are immutable for the lifetime of the `KaModule` and scope refiners must be consistent with that. So scope refiners must create the same enlargement and restriction scopes for the same `KaModule`. (This restriction should be documented in `KotlinContentScopeRefiner`.)

The recently added `KotlinResolutionScopeEnlarger` should be removed in favor of and covered by `KotlinContentScopeRefiner`.

#### Interface sketch (analysis-api-platform-interface)

```kotlin
package org.jetbrains.kotlin.analysis.api.platform.projectStructure

public interface KotlinContentScopeRefiner : KotlinPlatformComponent {
    public fun getEnlargementScopes(module: KaModule): List<GlobalSearchScope> = emptyList()
    public fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> = emptyList()
}
```

### Resolve extensions

Resolve extensions hook into the refining mechanism:

* Negated shadowed scopes are applied as restriction scopes, i.e. the content scope is restricted to all files which are not in the shadowed scope.  
* A resolve extension also enlarges the content scope with its own generated files.  
  * This will be handled in a follow-up issue: [KT-74541](https://youtrack.jetbrains.com/issue/KT-74541) Analysis API: Include files generated by resolve extensions in \`KaModule\` content scopes

To support this, we would implement a `KotlinContentScopeRefiner` for resolve extensions in the Analysis API, likely in `analysis-api-impl-base`.

## Content scope computation

We implement a `KaContentScopeProvider` which calculates a module’s content scope from the base scope and associated `KotlinContentScopeRefiner`s.

`KaContentScopeProvider` should be an `analysis-api` service so that we can have a default implementation for `KaModule.contentScope`. It needs to be marked with `@KaPlatformInterface`.

## Resolution scope computation

We implement a `KaResolutionScopeProvider` which calculates a resolution scope from the module’s content scope and the content scopes of all dependencies using a fixed algorithm.

`KaResolutionScopeProvider` should be an `analysis-api` service so that users can get a resolution scope for the `KaModule`. Alternatively, we can introduce a property `KaModule.resolutionScope` which uses `KaResolutionScopeProvider` as a default implementation. Then the provider mirrors `KaContentScopeProvider` and needs to be marked with `@KaPlatformInterface`.

The current `KotlinResolutionScopeProvider` platform component should be removed. Its usages need to be replaced with `KaResolutionScopeProvider`. Its implementations should become the basis for the implementation of `KaResolutionScopeProvider`.

The scope created by `KaResolutionScopeProvider` should be a modified implementation of the current `KaGlobalSearchScope`, as it needs to [additionally check for generated files](https://youtrack.jetbrains.com/issue/KT-73290/Analysis-API-Improve-the-architecture-of-content-scopes-and-resolution-scopes#focus=Comments-27-11400711.0-0).

### Source scope optimization in IdeKotlinByModulesResolutionScopeProvider

`IdeKotlinByModulesResolutionScopeProvider` contains special handling for source modules in `excludeIgnoredModulesByKotlinProjectModel`. This special handling, especially the usage of `moduleWithDependenciesAndLibrariesScope`, should be removed completely.

It was a performance optimization for inefficient union resolution scopes, which shouldn’t be necessary anymore now that we have scope merging. And in any case, this implementation would be incorrect in the new model, since it would disregard the adjusted content scopes of individual modules.

The new `KaResolutionScopeProvider` does not need to handle ignored source module dependencies (like in `excludeIgnoredModulesByKotlinProjectModel`) since dependencies are already ignored when they’re calculated in `KtSourceModuleByModuleInfo`.
