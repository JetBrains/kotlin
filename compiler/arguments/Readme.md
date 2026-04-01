## Kotlin compiler arguments

Contains a unified representation of Kotlin compiler arguments for current and old Kotlin releases.

One representation is a Kotlin-based - check `org.jetbrains.kotlin.arguments.description.compilerArguments`.
Another one representation is JSON-based that is bundled into a published jar as `kotlin-compiler-arguments.json` file.

### JSON schema changelog

- `1`: Initial schema
- `2`: Extended `AllKotlinArgumentTypes` with `klibIrInlinerMode`. New argument value type: `KlibIrInlinerModeType`.
- `3`:
    - Added `affectsCompilationOutcome` and `argumentType` fields to `KotlinCompilerArgument`.
    - Added `modifiers` field to `KotlinCompilerArgumentsLevel` with new `Modifier` enum (`DEPRECATED`, `SEALED`).
    - Extended `AllKotlinArgumentTypes` with new enum types: `JvmDefaultMode`, `AbiStabilityMode`, `AssertionsMode`,
      `JspecifyAnnotationsMode`, `LambdasMode`, `SamConversionsMode`, `StringConcatMode`, `CompatqualAnnotationsMode`,
      `WhenExpressionsMode`, `JdkRelease`, `AnnotationDefaultTargetMode`, `NameBasedDestructuringMode`, `VerifyIrMode`.
    - New argument value types: `PathType`, `StringListType`, `SearchPathType`, `PathListType`, `EnumType`.
