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

## Beyond Assert

The plugin by default will transform `assert` function calls but can also
transform other functions like `require`, `check`, `assertTrue`, and many, many
more.

Functions which can be transformed have specific requirements. A function must
have a form which allows taking a `String` or `() -> String` value as the last
parameter. This can either be as an overload or the original function.

For example, the `assert` function has 2 definitions:
* `fun assert(value: Boolean)`
* `fun assert(value: Boolean, lazyMessage: () -> Any)`

If the first function definition is called, it will be transformed into calling
the second definition with the diagram message supplied as the last parameter.
If the second definition is called, it will be transformed into calling the same
function but with the diagram message appended to the last parameter.

This transformed function call doesn't need to throw an exception either. See
[Advanced Usage](#advanced-usage) for some examples.

## Gradle Plugin

Builds of the Gradle plugin are available through the
[Gradle Plugin Portal][kotlin-power-assert-gradle].

```kotlin
plugins {
  kotlin("multiplatform") version "1.8.20"
  id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
}
```

The Gradle plugin allows configuring the functions which should be transformed
with a list of fully-qualified function names.

```kotlin
// Kotlin DSL
configure<com.bnorm.power.PowerAssertGradleExtension> {
  functions = listOf("kotlin.assert", "kotlin.test.assertTrue")
}
```

```groovy
// Groovy
kotlinPowerAssert {
  functions = ["kotlin.assert", "kotlin.test.assertTrue"]
}
```

You can also exclude Gradle source sets from being transformed by the plugin,
where those source sets can be specified by name.

```kotlin
// Kotlin DSL
configure<com.bnorm.power.PowerAssertGradleExtension> {
  excludedSourceSets = listOf(
    "commonMain",
    "jvmMain",
    "jsMain",
    "nativeMain"
  )
}
```

```groovy
// Groovy
kotlinPowerAssert {
  excludedSourceSets = [
    "commonMain",
    "jvmMain",
    "jsMain",
    "nativeMain"
  ]
}
```

## Compatibility

The Kotlin compiler plugin API is unstable and each new version of Kotlin can
bring breaking changes to the APIs used by this compiler plugin. Make sure you
are using the correct version of this plugin for whatever version of Kotlin
used. Check the table below to find when support for a particular version of
Kotlin was first introduced. If a version of Kotlin or this plugin is not listed
it can be assumed to maintain compatibility with the next oldest version listed.

| Kotlin Version | Plugin Version |
|----------------|----------------|
| 1.3.60         | 0.1.0          |
| 1.3.70         | 0.3.0          |
| 1.4.0          | 0.4.0          |
| 1.4.20         | 0.6.0          |
| 1.4.30         | 0.7.0          |
| 1.5.0          | 0.8.0          |
| 1.5.10         | 0.9.0          |
| 1.5.20         | 0.10.0         |
| 1.6.0          | 0.11.0         |
| 1.7.0          | 0.12.0         |
| 1.8.20         | 0.13.0         |

## Kotlin IR

This plugin supports all IR based compiler backends: JVM, JS, and Native! Only
Kotlin/JS still uses the legacy compiler backend by default, use the following
to make sure IR is enabled.

```kotlin
target {
  js(IR) {
  }
}
```

## Advanced Usage

### Function Call Tracing

Similar to Rust's `dbg!` macro, functions which take arbitrary parameters can
be transformed. For example:

```kotlin
fun <T> dbg(value: T): T = value

fun <T> dbg(value: T, msg: String): T {
  println(msg)
  return value
}

fun main() {
  println(dbg(1 + 2 + 3))
}
```

Prints the following:

```text
dbg(1 + 2 + 3)
      |   |
      |   6
      3
6
```

### Soft Assertion

To achieve soft assertion, the following template can be implemented:

```kotlin
typealias LazyMessage = () -> Any

interface AssertScope {
  fun assert(assertion: Boolean, lazyMessage: LazyMessage? = null)
}

fun <R> assertSoftly(block: AssertScope.() -> R): R = TODO("implement")
```

You can then use the template as follows:

```kotlin
val jane: Person = TODO()
assertSoftly {
  assert(jane.firstName == "Jane")
  assert(jane.lastName == "Doe")
}
```

A working example is [available][soft-assert-example] in this repository in the
sample directory.

[groovy-power-assert]: https://groovy-lang.org/testing.html#_power_assertions
[kotlin-power-assert-gradle]: https://plugins.gradle.org/plugin/com.bnorm.power.kotlin-power-assert
[soft-assert-example]: https://github.com/bnorm/kotlin-power-assert/blob/master/sample/src/commonMain/kotlin/com/bnorm/power/AssertScope.kt
