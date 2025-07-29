# Branch: junie/KT-22815

## Overview

This branch adds tests for the `FirSerializationTypeAliasChecker` in the Kotlin Serialization compiler plugin. The checker reports warnings when the `kotlinx.serialization.Serializable` annotation is used as a typealias in Kotlin code compiled with K2 (the new Kotlin compiler).

## Background

The K2 compiler does not expand annotation typealiases on the `COMPILER_REQUIRED_ANNOTATIONS` phase. This means that when the `Serializable` annotation is used through a typealias, it might not be properly processed by the serialization plugin, potentially leading to unexpected behavior.

## Changes in this Branch

This branch adds a comprehensive test file (`typeAliasedSerializable.fir.kt`) that verifies the correct behavior of the `FirSerializationTypeAliasChecker`. The test file includes:

1. A typealias for the `Serializable` annotation:
   ```kotlin
   typealias MySerializable = Serializable
   ```

2. Normal usage of the `Serializable` annotation (which should not trigger a warning):
   ```kotlin
   @Serializable
   class NormalClass(val value: String)
   ```

3. Usage of the typealiased `Serializable` annotation (which should trigger a warning):
   ```kotlin
   @MySerializable
   class TypeAliasedClass(val value: String)
   ```

4. Usage of the typealiased `Serializable` annotation with a custom serializer (which should also trigger a warning):
   ```kotlin
   @MySerializable(CustomSerializer::class)
   class TypeAliasedClassWithCustomSerializer(val value: String)
   ```

The test file includes the expected diagnostic markers (`<!TYPEALIASED_SERIALIZABLE_ANNOTATION!>`) to indicate where the warnings should be reported.

## Implementation Details

The `FirSerializationTypeAliasChecker` is implemented in the Kotlin Serialization compiler plugin and is responsible for detecting and reporting warnings when the `Serializable` annotation is used through a typealias. The checker:

1. Examines typealias declarations
2. Checks if the expanded type is `kotlinx.serialization.Serializable`
3. Reports a warning on the typealias declaration if it expands to `Serializable`

## Why This Matters

This warning helps developers avoid potential issues when using the Serialization plugin with K2. By warning about typealiased `Serializable` annotations, it guides developers toward using the annotation directly, ensuring proper processing by the compiler.

## Warning Message

When a developer uses a typealiased `Serializable` annotation, they will see the following warning:

```
Typealiased kotlinx.serialization.Serializable annotation is not expanded on the COMPILER_REQUIRED_ANNOTATIONS phase in K2 compiler. Consider using @MetaSerializable instead.
```

The warning not only explains the issue but also suggests using `@MetaSerializable` as an alternative approach for creating custom serializable annotations.

## Related Issues

- [KT-22815](https://youtrack.jetbrains.com/issue/KT-22815): Add tests for FirSerializationTypeAliasChecker warnings