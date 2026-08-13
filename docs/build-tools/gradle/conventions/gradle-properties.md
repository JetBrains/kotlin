## Naming conventions

This section outlines the official naming conventions for Kotlin-specific [gradle project properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties)
that users could define in the `gradle.properties` file, command line arguments, environment variables and other locations specified by Gradle.
Following these standards ensures consistency across the ecosystem and reduces a cognitive load for both developers and users.

1. The Root Namespace – all properties introduced by the Kotlin Gradle Plugin must start with the standard `kotlin.` prefix.
2. Internal vs. Public Properties – to distinguish between user-facing configurations and internal mechanics, we use a dedicated `kotlin.internal.*` namespace:
   - Properties intended for internal use by the Kotlin team and Kotlin ecosystem projects (e.g., Compose, kotlinx libraries) for debugging,
tweaking and verification purposes. Other users should avoid using them.
3. Experimental Features - Kotlin Gradle plugins should not use `kotlin.experimental.*` or `kotlin.unsafe.*`  for experimental features. 
Instead, experimental features are managed through tooling diagnostics and DSL Opt-ins:
    - Runtime Diagnostics: Every experimental property must trigger a tooling diagnostic.
    - Naming: The diagnostic name and ID must explicitly contain the word "Experimental".
    - Corresponding DSL elements should be annotated with `@ExperimentalKotlinGradleApi` and follow the standard deprecation lifecycle.
4. Case Convention – Kotlin plugins should use a lower camel case approach for the property name following the namespace.
Example: `kotlin.enableKmpCInteropCommonization`
5. Namespace Strategy - besides the `kotlin.internal.*` namespace, only platform-specific namespaces are allowed:
    - `kotlin.native.*`
    - `kotlin.jvm.*`
    - `kotlin.js.*`
    - `kotlin.wasm.*`
    - `kotlin.android.*`

Other existing namespaces will be kept for backward compatibility, but no new properties should be added into them.

### Generic naming guidelines

- When naming a property, the primary goal is to reduce a cognitive load
    - ❌ Bad: `kotlin.mppStabilityNowarn`
    - ✅ Good: `kotlin.suppressMultiplatformStabilityWarnings`
- The name should describe the action or capability. While matching the default value is a secondary criterion, clarity of the intent is
  paramount.
    - ❌ Bad: `kotlin.native.binaryGc` (unclear what value enables/disables)
    - ✅ Good: `kotlin.native.binaryEnableGarbageCollector`
- Avoid "double negatives" (e.g., `notDisableSomething`).
    - ❌ Bad: `kotlin.js.notDisableSourceMaps`
    - ✅ Good: `kotlin.js.generateSourceMaps`
- Optionally, before finalizing a property name, use the following validation steps:
    - Provide the proposed name to an AI without context and ask it to explain what the property does. If the explanation is incorrect, the
      name is too ambiguous.
    - Propose three naming alternatives to Gradle power users to gauge understandability.
