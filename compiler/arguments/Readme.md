## Kotlin compiler arguments

Contains a unified representation of Kotlin compiler arguments for current and old Kotlin releases.

One representation is a Kotlin-based - check `org.jetbrains.kotlin.arguments.description.compilerArguments`.
Another one representation is JSON-based that is bundled into a published jar as `kotlin-compiler-arguments.json` file.

### JSON schema changelog

- `1`: Initial schema
- `6`: No schema changes from `5` other than order changed during schema serialization