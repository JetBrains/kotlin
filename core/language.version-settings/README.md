# Kotlin Language Version Settings

Language and API versioning configuration for the Kotlin compiler. This module provides `LanguageVersion`, `ApiVersion`, `LanguageFeature`,
and `LanguageVersionSettings` used by the compiler frontend, the analysis infrastructure, and build tooling to control which language features
are enabled for a given compilation.

The module is exposed as a part of the Kotlin Analysis API. Unless your intent is to add new API for the Kotlin Analysis API clients, you
should avoid putting any new code in this module.

## Binary Compatibility

While the module can depend on other utility modules of the compiler, its API must be free of such dependencies. The compatibility validator
plugin and the foreign class usage tracker ensure that there are no unexpected exposures.
