// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-63756

// MODULE: common
// FILE: Expect.kt
expect annotation class Ignore

// MODULE: main()()(common)
// FILE: org/junit/Ignore.java

package org.junit;

public @interface Ignore {}

// FILE: Actual.kt
actual typealias Ignore = org.junit.Ignore

@Ignore
class Test {
    fun ok() = "OK"
}

fun box() = Test().ok()
