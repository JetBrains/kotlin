# Kotlin Language Targets: Common

The multiplatform (common) compilation target. This module provides `CommonPlatforms`, which combines the JVM, JS, Wasm, and Native
platforms of the sibling `:core:language.targets.*` projects.

The module is exposed as a part of the Kotlin Analysis API. Unless your intent is to add new API for the Kotlin Analysis API clients, you
should avoid putting any new code in this module.

## Binary Compatibility

While the module can depend on other utility modules of the compiler, its API must be free of such dependencies. The compatibility validator
plugin and the foreign class usage tracker ensure that there are no unexpected exposures.
