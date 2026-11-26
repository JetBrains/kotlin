# Kotlin Language Targets

Platform abstraction layer for Kotlin compilation targets. This module provides core types such as `TargetPlatform`, `SimplePlatform`, and
abstract platform classes (`JsPlatform`, `NativePlatform`, `WasmPlatform`) used by the compiler, the analysis infrastructure, and build
tooling for multiplatform target resolution. The platform implementations are available in separate projects: `:core:language.targets.jvm`,
`:core:language.targets.js`, `:core:language.targets.wasm`, `:core:language.targets.native`, and `:core:language.targets.common`.

The module is exposed as a part of the Kotlin Analysis API. Unless your intent is to add new API for the Kotlin Analysis API clients, you
should avoid putting any new code in this module.

## Binary Compatibility

While the module can depend on other utility modules of the compiler, its API must be free of such dependencies. The compatibility validator
plugin and the foreign class usage tracker ensure that there are no unexpected exposures.
