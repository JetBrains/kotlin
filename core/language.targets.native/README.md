# Kotlin Language Targets: Native

Native-specific compilation target configuration. This module provides `NativePlatformWithTarget`, `NativePlatformUnspecifiedTarget`, and
`NativePlatforms` used by the Native backend, the analysis infrastructure, and build tooling. The targets themselves (`KonanTarget`) come
from `:native:kotlin-native-targets`.

The module is exposed as a part of the Kotlin Analysis API. Unless your intent is to add new API for the Kotlin Analysis API clients, you
should avoid putting any new code in this module.

## Binary Compatibility

While the module can depend on other utility modules of the compiler, its API must be free of such dependencies. The compatibility validator
plugin and the foreign class usage tracker ensure that there are no unexpected exposures.
