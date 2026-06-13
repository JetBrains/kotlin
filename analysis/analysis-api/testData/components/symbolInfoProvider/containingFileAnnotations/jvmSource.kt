// FILE: other.kt
@file:Anno
package org.example

@Target(AnnotationTarget.FILE)
annotation class Anno

fun other() {}

// FILE: main.kt
import org.example.*

fun main() {
    other()
}
