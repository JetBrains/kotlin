# Java-Direct Module: General Code Quality

## Overview
The codebase demonstrates solid technical depth and domain understanding of the Java language rules and FIR constraints. However, as the semantic complexity evolved, it accumulated into dense files, prioritizing implementation strength over maintainability.

## Main Quality Issues and Problematic Places

### 1. `JavaResolutionContext.kt` is Overburdened
This file acts as a "God class". It combines local-class lookup logic, central dotted-name resolution paths, inherited inner-class resolution, and context creation/import extraction.
- **Problem**: This centralization makes invariants difficult to reason about and pieces hard to test independently. Fixing one resolution path can easily perturb another. 
- **Pointer**: E.g., `JavaResolutionContext.kt:25-61` (constructor responsibilities) and `375-480` (central dotted-name resolution).

### 2. `JavaClassFinderOverAstImpl.kt` Abstraction Coupling
This file handles filesystem walking, source index building, lightweight scanning, full parsing fallback, supertype analysis, and inherited inner-class collection.
- **Problem**: Carrying multiple architectural roles inside one class leads to tight coupling. 
- **Pointer**: E.g., Index building at `259-278`, eager parse/cache at `325-358`, and supertype graph support at `455-559`.

### 3. Heuristics and Convention-Driven Behaviors
Several resolution operations rely on heuristics.
- **Problem**: There is separate behavior for small vs large files, and canonical file-name filtering for exposed classes. These conventions create fragile points during long-term evolution.
- **Pointer**: `CombinedJavaClassFinder.kt:38-49` contains FQN verification logic with a `TODO` actively questioning the reasoning.

### 4. Unfinished Edges and Error Swallowing
- **Problem**: `JavaClassFinderOverAstImpl.kt` contains `tryReadFile` which silently swallows I/O problems and returns `null`. This blurs the distinction between a missing class, an unreadable file, and a failed parse operation, complicating error diagnosis.
- **Pointer**: Scattered typos (`shoulbe`, `enore`) and unresolved `TODO` comments (e.g., debug-log property in `JavaDirectComponentRegistrar.kt:63`) signal review debt.

### 5. Fragile Invariants Tied to Object Identity
- **Problem**: Semantic constraints are sometimes subtly tied to object lifecycle rather than explicit APIs. For instance, `parseTopLevelClassFromFile(...)` in `JavaClassFinderOverAstImpl` relies on reusing the exact same `JavaClassOverAst` instance to preserve type-parameter identity. Such invariants are easily broken by refactoring since they depend entirely on comments rather than structural enforcement.

## Suggested Refactoring Directions
To sustain long-term evolution, focus on structural decomposition over cosmetic cleanup:
- **Decompose `JavaResolutionContext`**: Split into an import/simple-name resolver, a nested/inherited-inner-class resolver, and a context factory.
- **Isolate Indexing**: Split `JavaClassFinderOverAstImpl` into a source index builder and a separate file parser/cache helper.
- **Solidify API Contracts**: Convert subtle object-identity expectations and fragile `TODO` comments into dedicated tests or narrower, explicit APIs.
