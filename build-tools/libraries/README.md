# Kotlin Build Tools libraries

Libraries shared by Kotlin build tool integrations: the Kotlin Gradle plugin (KGP), the Kotlin
Maven plugin, JPS, kotlin-toolchain, Bazel rules, and whatever else is built on the
[Build Tools API](../../compiler/build-tools/README.md) (BTA). None of them depend on a particular build tool.

BTA (`org.jetbrains.kotlin:kotlin-build-tools-api`) is the interface the compiler exposes to build
tools. The libraries here sit on top of it. They hold the logic each integration would otherwise
have to write for itself.

The libraries ship with Kotlin releases and are meant for Kotlin's build tool integrations, which
use the library version released with them. The API can change in any Kotlin release, without a
deprecation cycle. Each library has an ABI dump that records every change, so changes are visible
in review.

## Layout

| What        | Value                                        |
|-------------|----------------------------------------------|
| Group       | `org.jetbrains.kotlin`                       |
| Artifact    | `build-tools-<tool>`                         |
| Directory   | `build-tools/libraries/<tool>`               |
| Gradle path | `:build-tools:libraries:<tool>`              |
| Package     | `org.jetbrains.kotlin.buildtools.<tool>`     |
| API dump    | `<tool>/api/<tool>.api`                      |

Like every artifact in the repository, the libraries use the group `org.jetbrains.kotlin`; the
`build-tools-` prefix keeps them together. The package is a sibling of BTA's
`org.jetbrains.kotlin.buildtools.api`.

Every module applies the `kotlin-build-tools-library` convention plugin, which lives in
`repo/gradle-build-conventions/kotlin-build-tools-library`. A library's own build script needs little more
than `id("kotlin-build-tools-library")` and a `description` (the POM description): the plugin takes care of the artifact name,
compiling the way KGP does, explicit API mode, publishing, JUnit 5 with `test-inputs-check` and
the API dump.

## Modules

None yet.

How to add one, what it may depend on and how to test it: see [AGENTS.md](AGENTS.md).
The rules the AI review checks for every library are in [library-rules.md](library-rules.md).
