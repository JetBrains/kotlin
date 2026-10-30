# No Gradle or Kotlin Gradle plugin dependencies

Applies to: `build.gradle.kts`

A Kotlin Build Tools library is shared by KGP, Maven, JPS, kotlin-toolchain and Bazel rules, so its build
script must not add `gradleApi()`, `gradleKotlinDsl()`, any `org.gradle:*` artifact or any
`kotlin-gradle-plugin*` project. Move the Gradle-dependent part back to KGP instead.
The convention plugin does not enforce this, so review has to.

# Apply the convention plugin, do not reconfigure it

Applies to: `build.gradle.kts`

The script applies `kotlin-build-tools-library` and sets `description`. It must not configure ABI
validation or `test-inputs-check` itself, and must not change the Kotlin language/API version, JVM
target or stdlib setup the plugin configures. BTA is added with
`api(project(":compiler:build-tools:kotlin-build-tools-api"))` when needed.

# Flag third-party dependencies

Applies to: `build.gradle.kts`

Every dependency ships with every integration, and inside Gradle another plugin may already have loaded
coroutines or serialization at a different version. A new dependency outside the Kotlin libraries
(kotlinx-coroutines, kotlinx-serialization, Ktor) or BTA should be questioned. When an implementation needs
third-party dependencies, the library should be split into an API module without them and an implementation
module the integration can load in isolation.

# Only BTA reaches the compiler, and no integration-specific APIs

Applies to: `**/src/main/**/*.kt`

Compiler access goes through `org.jetbrains.kotlin.buildtools.api`. Imports of compiler internals, Gradle
(`org.gradle.*`), Maven, JPS or IntelliJ APIs are not allowed here; anything specific to one integration
stays in that integration.

# Public API has KDoc and an updated dump

Applies to:
```
**/src/main/**/*.kt
**/api/*.api
```

Every public declaration has KDoc. A change that adds, removes or alters a public declaration must come
with the matching change to `api/<tool>.api` in the same diff; `check` fails on a stale dump, but the
dump diff is also what reviewers read to see what consumers can observe.

# Plain types at the boundary

Applies to: `**/src/main/**/*.kt`

Public API uses plain Kotlin/JDK types: `java.nio.file.Path`, `String`, collections, data or value
classes. No lazy Gradle-style wrappers such as providers or properties. Collaborators (for example a BTA
`KotlinLogger`) are passed in as parameters, not read from global state. Exceptions are specific and
their messages say what went wrong.

# Behaviour changes come with tests

Applies to: `**/src/main/**/*.kt`

A change that adds a public entry point or alters behaviour includes tests under `src/test/kotlin`,
covering the main path, edge cases (empty input, missing or duplicate files, paths with spaces or
non-ASCII characters) and failure paths with the exception type and message. A bug fix includes a test
that fails without it.

# Tests stay Gradle-free and self-contained

Applies to: `**/src/test/**/*.kt`

Tests use JUnit 5 with `kotlin-test` assertions and `@TempDir` for files. They do not use Gradle TestKit
or `ProjectBuilder`, do not read the repository layout, the user's home or the network, do not share
mutable state, and do not sleep. BTA interfaces are faked; real compilation tests are the exception.
