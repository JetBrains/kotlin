# kotlin-power-assert

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.bnorm.power/kotlin-power-assert-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.bnorm.power/kotlin-power-assert-plugin)

Kotlin Compiler Plugin which high-jacks Kotlin assert function calls and
transforms them similar to [Groovy's Power Assert feature][groovy-power-assert].
This plugin uses the IR backend for the Kotlin compiler and supports all
platforms: JVM, JS, and Native!

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
  kotlin("jvm") version "1.4.20"
  id("com.bnorm.power.kotlin-power-assert") version "0.5.3"
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
1.4.20 and IR is enabled. This includes all IR based compiler backends: JVM, JS,
and Native! As Kotlin IR is still experimental, mileage may vary.

##### Kotlin/JVM
```kotlin
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    useIR = true
  }
}
```

##### Kotlin/JS
```kotlin
target {
  js(IR) {
  }
}
```

##### Kotlin/Native
IR already enabled by default!

[groovy-power-assert]: https://groovy-lang.org/testing.html#_power_assertions
[kotlin-power-assert-gradle]: https://plugins.gradle.org/plugin/com.bnorm.power.kotlin-power-assert
