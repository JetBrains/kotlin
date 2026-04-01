# Kotlin Language Targets: JVM

JVM-specific compilation target configuration. This module provides `JvmTarget` (the enumeration of supported JVM bytecode versions),
`JdkPlatform`, and `JvmPlatforms` used by the JVM backend, the analysis infrastructure, and build tooling for JVM bytecode version selection.

The module is exposed as a part of the Kotlin Analysis API. Unless your intent is to add new API for the Kotlin Analysis API clients, you
should avoid putting any new code in this module.

## Binary Compatibility

While the module can depend on other utility modules of the compiler, its API must be free of such dependencies. The compatibility validator
plugin and the foreign class usage tracker ensure that there are no unexpected exposures.
