# Kotlin Build Tools API (BTA)

An experimental interface for build systems (Gradle plugin, Maven plugin, etc.) to invoke Kotlin compilation without a direct compiler
dependency. Build systems should use the API from `kotlin-build-tools-api`, load the implementation in an isolated ClassLoader, and avoid
accessing compiler internals directly.

## Modules

| Module                                                                                                     | Purpose                                                                                        |
|------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| [`kotlin-build-tools-api`](kotlin-build-tools-api)                                                         | Public interfaces only; no implementation; `explicitApi()`; API dump checked in                |
| [`kotlin-build-tools-impl`](kotlin-build-tools-impl)                                                       | Default implementation; version-coupled to the compiler; must run in isolated ClassLoader      |
| [`kotlin-build-tools-compat`](kotlin-build-tools-compat)                                                   | Adapter for compilers < 2.3.0 (wraps deprecated `CompilationService`)                          |
| [`kotlin-build-tools-cri-impl`](kotlin-build-tools-cri-impl)                                               | Protobuf serialization for Compiler Reference Index; shipped as shadow JAR                     |
| [`kotlin-build-tools-jdk-utils`](kotlin-build-tools-jdk-utils)                                             | Internal utility for Java 9+ platform ClassLoader detection; do not use outside BTA modules    |
| [`kotlin-build-tools-options-generator`](kotlin-build-tools-options-generator)                             | KotlinPoet-based code generator producing compiler argument classes from `:compiler:arguments` |
| [`kotlin-build-statistics`](kotlin-build-statistics)                                                       | Shared library for build metric collection (times, performance, GC, attributes); used by impl and KGP |
| [`util-kotlinpoet`](util-kotlinpoet)                                                                       | KotlinPoet utility helpers shared by code generators in the BTA area                           |
| [`kotlin-build-tools-api-tests`](kotlin-build-tools-api-tests)                                             | Main integration test suite (JUnit 5, multiple named test suites)                              |
| [`kotlin-build-tools-api-forward-compatibility-tests`](kotlin-build-tools-api-forward-compatibility-tests) | Tests the forward compatibility guarantee (X+1)                                                |

## Architecture: ClassLoader Isolation

The impl JAR must be loaded in an isolated ClassLoader to prevent classpath conflicts with the consumer:

```kotlin
val toolchains = KotlinToolchains.loadImplementation(implClasspath)  // implClasspath: List<Path>
```

- `loadImplementation(List<Path>)` — preferred API; wraps the classpath in a `URLClassLoader` backed by `SharedApiClassesClassLoader` automatically
- `loadImplementation(ClassLoader)` — lower-level overload for custom ClassLoader setups; the ClassLoader's parent should be `SharedApiClassesClassLoader`
- The impl JAR version must match the compiler version (`kotlin-build-tools-impl` is version-coupled to the compiler)
- For compilers < 2.3.0, include `kotlin-build-tools-compat` in the impl classpath → see [`kotlin-build-tools-compat/README.md`](kotlin-build-tools-compat/README.md)

## Key Abstractions

All in `kotlin-build-tools-api/src/main/kotlin/org/jetbrains/kotlin/buildtools/api/`:

- `KotlinToolchains` — factory entry point; creates `BuildSession` instances (`KotlinToolchains.kt`)
- `BuildSession` (`AutoCloseable`) — manages caches, thread pools, and daemon connections
- `Toolchain` (sealed interface) — `JvmPlatformToolchain`, `CriToolchain`, `AbiValidationToolchain`
- `BuildOperation<R>` / `BuildOperation.Builder` — type-safe operation configuration (`BuildOperation.kt`)
- `ExecutionPolicy` — `InProcess` vs `WithDaemon` (`ExecutionPolicy.kt`)
- `KotlinLogger` — pluggable logging interface (`KotlinLogger.kt`)

## Generated Files

Do not edit generated files manually — regenerate them with the tasks below.

Two kinds — both must be regenerated after relevant changes:

```bash
# Regenerate compiler argument classes (after changing compiler arguments in :compiler:arguments)
./gradlew :compiler:build-tools:kotlin-build-tools-api:generateBtaArguments
./gradlew :compiler:build-tools:kotlin-build-tools-impl:generateBtaArguments
./gradlew :compiler:build-tools:kotlin-build-tools-compat:generateBtaArguments

# Regenerate API binary compatibility dump (after any public API change)
./gradlew :compiler:build-tools:kotlin-build-tools-api:apiDump
```

## Compatibility Model

```
BTA version X is guaranteed to work with implementation versions [X-3, X+1]
```

- **Backward compat (X-3):** tested in `kotlin-build-tools-api-tests` compatibility suites (one suite per listed version)
- **Forward compat (X+1):** tested in `kotlin-build-tools-api-forward-compatibility-tests`
- When adding an API change that may break compatibility, add tests to both modules and run locally to verify

## Running Tests

```bash
# Run all tests (testExample is excluded from check — run it explicitly if needed)
./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:check

# Run against a specific BTA impl version (pattern: testCompatibility<version>)
./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testCompatibility2.3.20

# Run against current snapshot impl
./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testCompatibilitySnapshot

# Classpath/module-path escaping edge cases
./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testEscapableCharacters

# Verify restricted arguments are rejected correctly
./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testRestrictedArguments

# Individual named test suites follow the pattern :test<SuiteName>
# The full list is in `businessLogicTestSuits` in kotlin-build-tools-api-tests/build.gradle.kts

# Forward compatibility tests
./gradlew :compiler:build-tools:kotlin-build-tools-api-forward-compatibility-tests:check
```

## Writing Tests

See [`kotlin-build-tools-api-tests/README.md`](kotlin-build-tools-api-tests/README.md) for full conventions. Key rules:

- All tests extend `BaseTest` (`src/main/kotlin/BaseTest.kt`)
- All compilation tests extend `BaseCompilationTest` (`src/main/kotlin/compilation/BaseCompilationTest.kt`)
- Add `@DisplayName` to both test class and methods
- Add `@TestMetadata` pointing to the relevant test data directory for IDE navigation
- Keep test classes small — tests run in parallel
- Use the scenario DSL for incremental compilation tests; see `src/testExample/kotlin/ExampleIncrementalScenarioTest.kt`
- Annotate strategy-agnostic tests with `@DefaultStrategyAgnosticCompilationTest`
- Add a new test suite by appending its name to `businessLogicTestSuits` in `build.gradle.kts`
- Compatibility suites (`testCompatibility*`): add tests sparingly — they run once per listed version

## Key Conventions and Pitfalls

- Every public API addition in `kotlin-build-tools-api` must include KDoc documentation
- Do not add implementation dependencies to `kotlin-build-tools-api` — it must stay implementation-free
- Do not use `kotlin-build-tools-jdk-utils` outside BTA modules (requires `@KotlinBuildToolsInternalJdkUtils` opt-in)
- After changing compiler arguments, always regenerate both `generateBtaArguments` and `apiDump`
