# JS IR backend incremental compilation

### Overview 

The package `org.jetbrains.kotlin.ir.backend.js.ic` is responsible for incremental code generation.
It incrementally transforms the klib IR into the final JavaScript code. 
In other words, the code in this package receives an already-built klib,
detects dirty files from the klib with the modified IR,
additionally identifies files for which the IR must be lowered again
(for example, all callers of the modified inline function must be lowered again),
runs the lowering pipeline for both the dirty files and additional dirty files, and in the end,
incrementally generates JavaScript code. All the logic here is based on the IR from the klib.

Note that incremental klib compilation (compiling `kt` files to `klib`) is a separate process handled in another place.

### Details

Here are the two main classes:

- Class [CacheUpdater](CacheUpdater.kt):
    - Responsible for detecting all dirty files. This is a crucial part of the process. It includes:
      - Identifying files with modified IR (modified code).
      - Maintaining direct and inverse dependency graphs (see the class `KotlinSourceFileExports` and its inheritors) to detect additional dirty files that require re-lowering.
    - Loads IR for the dirty files.
    - Instantiates the compiler.
    - Runs the lowering pipeline for the dirty files.
    - Transforms the lowered IR to JS AST (see `JsIrProgramFragments`).
    - Maintains the cache files on disk (see class [IncrementalCache](IncrementalCache.kt)).

- Class [JsExecutableProducer](JsExecutableProducer.kt):
    - Responsible for incrementally producing JavaScript code from the JS AST (see `JsIrProgramFragments`, usually obtained from `CacheUpdater`, contained in `SrcFileArtifact` as a main part of `ModuleArtifact`).
    - Maintains the cache files (different from those in `CacheUpdater`) on disk (see classes [JsPerFileCache](JsPerFileCache.kt) and [JsPerModuleCache](JsPerModuleCache.kt)).
