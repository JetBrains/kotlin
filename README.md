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

## Kotlin IR

Using this compiler plugin only works if the code is compiled using IR. This can
be enabled only when compiling the test SourceSet if desired. As Kotlin IR is
still experimental, mileage may vary.

```groovy
compileTestKotlin {
    kotlinOptions {
        useIR = true
    }
}
```

## Project Snapshots

Snapshot builds of the Kotlin compiler plugin are available through Sonatype
Snapshot repository. Builds of the Gradle plugin are also available through the
[Gradle Plugin Portal][kotlin-power-assert-gradle].

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

repositories {
  // Required to find Kotlin compiler plugin as it is not bundled with Gradle plugin
  maven {
    url "https://oss.sonatype.org/content/repositories/snapshots"
  }
}
```

[groovy-power-assert]: https://groovy-lang.org/testing.html#_power_assertions
[kotlin-power-assert-gradle]: https://plugins.gradle.org/plugin/com.bnorm.power.kotlin-power-assert
