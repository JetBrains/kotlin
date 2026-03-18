## Kotlin compiler arguments

Contains a unified representation of Kotlin compiler arguments for current and old Kotlin releases.

One representation is a Kotlin-based - check `org.jetbrains.kotlin.arguments.description.compilerArguments`.
Another one representation is JSON-based that is bundled into a published jar as `kotlin-compiler-arguments.json` file.

### JSON schema changelog

- `1`: Initial schema
- `6`: No schema changes from `5` other than order changed during schema serialization
- `7`: Revert deprecation of `valueType` and `valueDescription`; mark `argumentType` as experimental
- `8`: Add `JvmDefaultModeType`
- `9`: Migrate compiler arguments to enum types: `AbiStabilityModeType`, `AssertionsModeType`, `JspecifyAnnotationsModeType`,
  `LambdasModeType`, `SamConversionsModeType`, `StringConcatModeType`, `CompatqualAnnotationsModeType`, `WhenExpressionsModeType`
