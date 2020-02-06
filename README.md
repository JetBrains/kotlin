# kotlin-power-assert

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

## Project Snapshots

Snapshot builds are available through Sonatype Snapshot repository.

```groovy
buildscript {
  repositories {
    maven {
      url "https://oss.sonatype.org/content/repositories/snapshots"
    }
  }
  dependencies {
    classpath "com.bnorm.power:kotlin-power-assert-gradle:0.1.0-SNAPSHOT"
  }
}

apply plugin: "com.bnorm.power.kotlin-power-assert"
```

[groovy-power-assert]: https://groovy-lang.org/testing.html#_power_assertions
