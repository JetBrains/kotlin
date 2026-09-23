# kotlin-reflect

This directory contains the JVM implementation of `kotlin-reflect`.

## Implementations

The main `kotlin-reflect` implementation is based on `kotlin-metadata-jvm` and Java reflection objects. It reads Kotlin metadata from the
classes being inspected and combines it with the corresponding Java reflection information.

There is also an older, K1-based implementation, built from parts of the K1 compiler. The legacy implementation is still used for collection
subclasses (KT-85727) and can be selected for compatibility or troubleshooting with the following system properties:

* `kotlin.reflect.jvm.useK1Implementation` — use the K1-based implementation for all callables.
* `kotlin.reflect.jvm.useK1ImplementationForMembers` — use it for Kotlin member functions, Kotlin member properties, and Java member 
  properties. This property is temporary and will be removed once we're sure that the new implementation has no regressions for these callables.
* `kotlin.reflect.jvm.loadMetadataDirectly` — make the new implementation load Kotlin metadata directly instead of obtaining parsed metadata
  from K1 descriptors. This behavior will be enabled by default once the K1 implementation is removed.

The K1-based implementation is temporary and is going to be removed. It's fine to fix bugs or support new features only in the new
implementation. Since tests run both for old and new implementations, sometimes it's necessary to check the value of the system property
in the test to exclude testing the old implementation:

```kotlin
if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) != true) {
    // ... (tests for the new implementation only)
}
```

## Debugging

By default, `kotlin-reflect.jar` is post-processed by several tools (shadow jar with relocation, strip metadata, ProGuard). This can cause
difficulties in debugging in the IDE. To avoid all post-processing, add this Gradle property to the test run command:

```text
-Pkotlin.build.postprocessing=false
```

## Tests

Run all kotlin-reflect tests with:

```text
./gradlew :compiler:fir:fir2ir:cleanTest :compiler:fir:fir2ir:test --tests 'org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBlackBoxCodegenTestGenerated$Box$Reflection' --tests 'org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBlackBoxCodegenTestGenerated$BoxJvm$Reflection' --tests 'org.jetbrains.kotlin.test.runners.codegen.ReflectionLegacyImplementationTestGenerated' --tests 'org.jetbrains.kotlin.test.runners.ir.FirLightTreeJvmIrTextTestGenerated' --tests 'org.jetbrains.kotlin.test.runners.codegen.ReflectionLoadMetadataDirectlyTestGenerated' --tests 'org.jetbrains.kotlin.test.runners.codegen.ReflectionK1MembersImplementationTestGenerated' :compiler:tests-integration:cleanTest :compiler:tests-integration:test --tests 'org.jetbrains.kotlin.reflection.ReflectionIntegrationTest'
```
