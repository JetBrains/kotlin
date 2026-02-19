# API Evolution

This guide covers the lifecycle of Analysis API endpoints from introduction to deprecation, including stability transitions and migration
strategies. For guidance on designing new APIs, see the [API Development Guide](api-development.md).

## Components of the Analysis API

The Analysis API consists of several interconnected components, each serving a specific purpose:

- **The Kotlin PSI** ([source](../../../compiler/psi/psi-api), [guidelines](../../../compiler/psi/AGENTS.md))
    - Foundation layer providing syntax tree representation through [`KtElement`](../../../compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtElement.kt)
    hierarchy
    - Uses `Kt` prefix (compared to `Ka` for Analysis API)
    - Has own stability annotations: `@KtExperimentalApi`, `@KtImplementationDetail`, `@KtNonPublicApi`
    - Key entities: [`KtFile`](../../../compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtFile.kt),
    [`KtDeclaration`](../../../compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtDeclaration.java),
    and [`KtExpression`](../../../compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtExpression.java)

- **Analysis API Surface** ([source](../../analysis-api))
    - User-facing layer of the Analysis API. Maps the Kotlin PSI to its semantic representation
    - Key entities: [`KaSession`](https://kotlin.github.io/analysis-api/fundamentals.html#kasession),
      [`KaSymbol`](https://kotlin.github.io/analysis-api/symbols.html), [`KaType`](https://kotlin.github.io/analysis-api/types.html)

- **Platform Interface** ([source](../../analysis-api-platform-interface))
    - Abstraction layer between the API and its execution environments
    - Defines how the API interacts with project structure and file systems
    - Key entities: [`KotlinPlatformComponent`](../../analysis-api-platform-interface/src/org/jetbrains/kotlin/analysis/api/platform/KotlinPlatformComponent.kt),
      [`KaEngineService`](../../analysis-api-platform-interface/src/org/jetbrains/kotlin/analysis/api/platform/KaEngineService.kt)

- **Analysis API Standalone** ([source](../../analysis-api-standalone))
    - Command-line implementation for using the API outside of IDEs
    - Provides project structure management that IDEs typically handle automatically

- **Implementations**
    - Analysis API implementations
        - K2 implementation, based on the new K2 compiler frontend ([source](../../analysis-api-fir))
        - K1 implementation, based on the classic, K1 compiler frontend ([source](../../analysis-api-fe10))
    - Platform interface implementations
        - Kotlin IntelliJ IDEA plugin ([source](https://github.com/JetBrains/intellij-community/tree/master/plugins/kotlin/base/analysis-api-platform))
        - Analysis API Standalone ([source](../../analysis-api-standalone))
    - PSI Implementation, including stubs ([source](../../../compiler/psi/psi-impl))
    - PSI Reference Implementations ([source](../../kt-references))

- **Light Classes** ([source](../../symbol-light-classes))
    - A Java view for Kotlin declarations designed mainly for Java interoperability
    - Currently, also used in UAST (technical debt)

- **Low-level API** ([source](../../low-level-api-fir))
    - K2-specific infrastructure for on-demand and incremental code analysis
    - Not directly accessible to end users

## API Stability Categories

Every part of the Analysis API falls under one of these stability categories:

- **Stable API**
    - Strong source and binary backward compatibility
    - Changes follow strict deprecation cycles
    - Applies to
        - *Analysis API Surface* (excluding declarations with opt-ins)

- **Unstable API**
    - May evolve without deprecation warnings
    - Changes *may* follow deprecation cycles, depending on the API usage
    - Applies to
        - *Platform Interface*
        - *Standalone API*
        - Declarations annotated with `@KaExperimentalApi`, `@KaNonPublicApi`, `@KaIdeApi`
        - *Kotlin PSI* is formally not yet stable despite its practical stability

- **Implementation Details**
    - No compatibility guarantees between versions
    - Reserved for internal use only
    - Applies to
        - *K1* and *K2* implementations of the Analysis API
        - *Platform Interface* implementations
        - *Low-level API*
        - *Light Classes* (unless explicitly exposed in the *Analysis API Surface*)
        - PSI Reference implementations
        - Declarations annotated with `@KaImplementationDetail`

> [!NOTE]
> PSI uses parallel annotations: `@KtExperimentalApi`, `@KtImplementationDetail`, `@KtNonPublicApi`.
> The `@KtPsiInconsistencyHandling` annotation marks code handling inconsistent PSI states (no Analysis API equivalent).

## Adding New APIs

New APIs follow a three-phase lifecycle designed to prevent premature stabilization while gathering real-world feedback.
This process protects both API designers and users from the cost of breaking changes.

### 1. Introduction Phase

**Goal**: Ship the API to early adopters while explicitly communicating its experimental nature.

- **Mark with `@KaExperimentalApi`**  
  This annotation serves as both a compiler warning and documentation that the API may change.
  Users must explicitly opt-in, ensuring they understand the risks.

- **Choose whether to implement for both K1 and K2**  
  If K1 support is impractical, use `@KaK1Unsupported` with clear documentation.

- **Provide comprehensive documentation and tests**  
  Experimental doesn't mean untested. Include edge cases, error conditions, and usage examples.
  This helps early adopters use the API correctly and provides valuable feedback.

- **Follow the design principles outlined in the [API Development Guide](api-development.md)**  
  Experimental APIs should still adhere to design principles.
  Fixing design issues later is more expensive than getting them right initially.

### 2. Validation Phase

**Goal**: Confirm the API design through real usage and identify necessary refinements.

- **Gather usage feedback**  
  Monitor how developers actually use the API versus intended usage.
  Look for common workarounds, frequent questions, or misuse patterns that suggest design issues.

- **Ensure idiomatic usage patterns emerge**  
  The API should feel natural to Kotlin developers.
  If users consistently need helper functions or complex setup code, consider incorporating these patterns into the API itself.

- **Verify edge case handling**  
  Real-world usage often reveals edge cases not covered in initial testing.
  Ensure the API gracefully handles malformed input, incomplete analysis results, and platform-specific variations.

### 3. Stabilization Phase

**Goal**: Ensure the API is ready for long-term compatibility commitments.

Before removing `@KaExperimentalApi`, verify that:

- **API is actively used with positive feedback**  
  Look for evidence that the API solves real problems efficiently.
  Lack of usage may indicate the API isn't needed anymore.

- **Naming is consistent and self-explanatory**  
  Names should align with Analysis API conventions and be understandable without extensive documentation.
  Consider whether parameter names can be improved based on usage patterns.

- **No anticipated design changes**  
  The team should have high confidence that the API design will remain stable.
  Major changes after stabilization require the full deprecation process.

## Deprecating APIs

Deprecation follows a careful multi-release cycle with clear migration paths.
The process balances giving users time to migrate while preventing indefinite maintenance of obsolete APIs.

### 1. Initial Deprecation

**Goal**: Signal the intent to remove the API while providing clear migration guidance.

- **Add the `@Deprecated(WARNING)` annotation**  
  This generates compiler warnings that alert users to the deprecation without breaking builds.
  The warning level ensures existing code continues to compile while encouraging migration.

- **Explain alternatives in the deprecation message and KDoc**  
  Provide guidance on replacement APIs. Include code examples when the migration is non-trivial.
  If no direct replacement exists, explain the recommended approach.

- **Update all usages in Kotlin and IntelliJ repositories**  
  Test the migration by updating internal usage.
  This serves as real-world examples for external users and ensures the migration path is practical.

- **Document migration in `COMPATIBILITY.md`**  
  Mention the deprecation in the compatibility document for smoother communication with the Analysis API users.

### 2. `ERROR` Advancement

**Goal**: Prevent new usage while maintaining both source and binary compatibility for existing code.

- **Ensure there's at least one major release with WARNING**  
  This gives users a full release cycle to discover and plan their migration.

- **Advance the deprecation level to `ERROR`**  
  This prevents new usage while allowing existing code to compile with explicit suppression.
  Users can still build their code but must acknowledge they're using deprecated APIs that will be removed shortly.

### 3. Removal or Hiding

**Goal**: Complete the deprecation process while maintaining ecosystem compatibility.

- **Ensure there's at least one major release with `ERROR`**  
  This provides a final grace period for users who need additional time to migrate complex codebases.

- **Choose between complete removal and `@Deprecated(HIDDEN)`**   
  Annotating by `@Deprecated(HIDDEN)` might be useful when the API is still needed for compatibility with already built JARs.