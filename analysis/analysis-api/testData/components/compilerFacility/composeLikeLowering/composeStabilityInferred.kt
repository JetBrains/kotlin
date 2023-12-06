// FILE: main.kt
package com.test

class A() {}

// FILE: lib.kt
package androidx.compose.runtime.internal

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class StabilityInferred(val parameters: Int)