# Gradle 9.3 Configuration Cache Issue with SwiftPM Import

## Problem Summary

Tests in `SwiftPMImportPersistentIdentifierPackageLockIntegrationTests.kt` fail when running with Gradle 9.3 and configuration cache enabled (`-P org.gradle.configuration-cache=true`). The error is:

```
org.gradle.internal.cc.base.exceptions.ConfigurationCacheError: Configuration cache state could not be cached: 
field `__dependencyIdentifierToImportedSwiftPMDependencies__` of task `:generateUmbrellaPackageIdentifierBasedResolutionForCommon` 
of type `org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject`: 
error writing value of type 'org.gradle.api.internal.provider.DefaultProperty'
```

## Root Cause

The `GenerateSyntheticLinkageImportProject` task has a property `dependencyIdentifierToImportedSwiftPMDependencies` that holds a Provider. This provider calls `.get()` on cross-project providers (from `SwiftPMLockTaskAggregationBuildService.buildAggregatedResultDependencies()`).

**Critical Issue**: Gradle's configuration cache serializer tries to evaluate ALL providers during cache serialization to determine if they're "fixed" values. When this happens:

1. The provider's `.get()` call is executed at **configuration time** (not execution time)
2. This calls `buildAggregatedResultDependencies()` which does:
   ```kotlin
   merged[selfIdentifier] = contribution.directMetadata.get()
   contribution.transitiveDependencies.get().metadataByDependencyIdentifier...
   ```
3. These `.get()` calls access cross-project providers without holding the project state lock
4. Result: "Current thread does not hold the state lock for project ':buzz'" error during serialization

## Why This is Hard to Fix

- **Even `@Internal` annotation doesn't prevent serialization**: Gradle still visits and tries to serialize properties marked `@Internal` during config cache
- **Cross-project access requires locks**: Gradle 9.3 enforces stricter thread-safety for cross-project access

## Attempts Made and Why They Failed

### Attempt 1: Change `@Input` to `@Internal`
```kotlin
@get:Internal
abstract val dependencyIdentifierToImportedSwiftPMDependencies: Property<TransitiveSwiftPMDependencies>
```
**Result**: FAILED - Gradle still serialized the property during config cache saving

## Key Code Locations

### Task Definition
- File: `/Users/berkay.bozkurt/kotlin/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/GenerateSyntheticLinkageImportProject.kt`
- Lines: 34-36 (the problematic property)
- Line: 127 (first `.get()` call in task action)
- Line: 130 (second `.get()` call in task action)

### Task Configuration
- File: `/Users/berkay.bozkurt/kotlin/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/SwiftImportSetupAction.kt`
- Lines: 245-248 (where the problematic provider is set)
- Line: 87, 118 (other task configurations)

### Aggregation Service
- File: `/Users/berkay.bozkurt/kotlin/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/syncPackageSwiftLock.kt`
- Lines: 146-179 (buildAggregatedResultDependencies method)
- Lines: 162, 164 (cross-project `.get()` calls that trigger lock errors)

### Test File
- File: `/Users/berkay.bozkurt/kotlin/libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/apple/SwiftPMImportPersistentIdentifierPackageLockIntegrationTests.kt`

## Solutions That Might Work

### Option 1: Remove Provider Wrapping Entirely
Instead of using a provider that aggregates cross-project dependencies, compute the aggregation result at configuration time (before cross-project access becomes problematic) OR make a simple provider that doesn't call `.get()`.

**Challenge**: Need to understand what prevents upfront computation. Is it that dependencies haven't been resolved yet?

### Option 2: Use a Different Serialization Strategy
- Create a custom codec that ignores this property
- Use Gradle's serialization annotations correctly (not kotlinx.serialization ones)
- Mark the property with an annotation that Gradle recognizes (if one exists)

### Option 3: Restructure the Aggregation
- Move aggregation logic to a different task or build service
- Don't make the aggregated result a task property
- Instead, compute it inside the `@TaskAction` using a mechanism that doesn't involve cross-project provider access

### Option 4: Defer Project Evaluation
Perhaps there's a way to delay the configuration of this task until after all cross-project dependencies are available and can be accessed without locks.


## Related Gradle Issues

This is likely related to Gradle's stricter configuration cache validation in 9.3. The error message "Current thread does not hold the state lock for project ':buzz'" suggests that Gradle 9.3 is enforcing thread-safety rules more strictly than previous versions.


