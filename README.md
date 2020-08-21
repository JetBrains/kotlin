# kotlin-power-assert

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.bnorm.power/kotlin-power-assert/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.bnorm.power/kotlin-power-assert)

Kotlin Compiler Plugin which high-jacks Kotlin assert function calls and
transforms them similar to [Groovy's Power Assert feature][groovy-power-assert].
This plugin uses the new IR backend for the Kotlin compiler.

## Example

Given following code:

```kotlin
val hello = "Hello"
assert(hello.length == "World".substring(1, 4).length)
```

Normally the assertion message would look like:

```text
java.lang.AssertionError: Assertion failed
	at <stacktrace>
```

A custom assertion message can be provided:

```kotlin
val hello = "Hello"
assert(hello.length == "World".substring(1, 4).length) { "Incorrect length" }
```

But this just replaces the message:

```text
java.lang.AssertionError: Incorrect length
	at <stacktrace>
```

With `kotlin-power-assert` included, the error message for the previous example
will be transformed:

```text
java.lang.AssertionError: Incorrect length
assert(hello.length == "World".substring(1, 4).length)
       |     |      |          |               |
       |     |      |          |               3
       |     |      |          orl
       |     |      false
       |     5
       Hello
	at <stacktrace>
```

Complex, multi-line, boolean expression are also supported:

```text
Assertion failed
assert(
  (text != null && text.toLowerCase() == text) ||
   |    |
   |    false
   null
      text == "Hello"
      |    |
      |    false
      null
)
```

## Gradle Plugin

Builds of the Gradle plugin are available through the
[Gradle Plugin Portal][kotlin-power-assert-gradle].

```kotlin
plugins {
  kotlin("jvm") version "1.4.0"
  id("com.bnorm.power.kotlin-power-assert") version "0.4.0"
}
```

The plugin by default will transform `assert` function call but can also
transform other functions like `require`, `check`, and/or `assertTrue`. The
function needs to validate the Boolean expression evaluates to `true` and has a
form which also takes a String or String producing lambda.

```kotlin
configure<com.bnorm.power.PowerAssertGradleExtension> {
  functions = listOf("kotlin.test.assertTrue", "kotlin.require")
}
``` 

## Kotlin IR

Using this compiler plugin only works if the code is compiled using Kotlin
1.4.0 and IR is enabled. IR can be enabled only when compiling the test
SourceSet if desired. As Kotlin IR is still experimental, mileage may vary.

```kotlin
compileTestKotlin {
    kotlinOptions {
        useIR = true
    }
}
```

[groovy-power-assert]: https://groovy-lang.org/testing.html#_power_assertions
[kotlin-power-assert-gradle]: https://plugins.gradle.org/plugin/com.bnorm.power.kotlin-power-assert
