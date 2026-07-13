# Naming conventions

This section outlines the official naming conventions for Kotlin-specific properties that users could define in the `gradle.properties` file.
Following these standards ensures consistency across the ecosystem and reduces cognitive load for both developers and users.

1. The Root Namespace - all properties introduced by the Kotlin Gradle Plugin must start with the standard `kotlin.` prefix.
2. Internal vs. Public Properties - to distinguish between user-facing configurations and internal mechanics, we use a dedicated `kotlin.internal.*` namespace:
   - Properties used exclusively by KGP for its internal logic.
   - Properties intended for use only within the Kotlin ecosystem (e.g., Compose, kotlinx libraries).
3. Experimental Features - Kotlin plugins should not use `kotlin.experimental.*` or `kotlin.unsafe.*`  for experimental features. 
Instead, experimental features are managed through tooling diagnostics and DSL Opt-ins:
    - Runtime Diagnostics: Every experimental property must trigger a tooling diagnostic.
    - Naming: The diagnostic name and ID must explicitly contain the word "Experimental".
    - Corresponding DSL elements should be annotated with `@ExperimentalKotlinGradleApi` and follow the standard deprecation lifecycle.
4. Case Convention - Kotlin plugins should use a lower camel case approach for the property name following the namespace.
Example: `kotlin.enableKmpCInteropCommonization`
5. Namespace Strategy - besides the `kotlin.internal.*` namespace, only platform-specific namespaces are allowed:
    - `kotlin.native.*`
    - `kotlin.jvm.*`
    - `kotlin.js.*`
    - `kotlin.wasm.*`
    - `kotlin.android.*`

Other existing namespaces will be kept for backward compatibility, but no new properties should be added into them.

#### Generic naming guidelines

- When naming a property, the primary goal is to reduce cognitive load
- The name should describe the action or capability. While matching the default value is a secondary criterion, clarity of the intent is paramount.
- Avoid "double negatives" (e.g., `notDisableSomething`).
- Optionally, before finalizing a property name, use the following validation steps:
  - Provide the proposed name to an AI without context and ask it to explain what the property does. If the explanation is incorrect, the name is too ambiguous.
  - Propose three naming alternatives to Gradle power users to gauge understandability.
