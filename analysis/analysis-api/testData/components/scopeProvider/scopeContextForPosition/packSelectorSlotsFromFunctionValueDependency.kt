// MODULE: lib
// FILE: lib.kt
package lib

@Target(AnnotationTarget.TYPE)
annotation class Composable

fun Button(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {}

val textButton: (text: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) -> Unit = ::Button

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.textButton

fun Wrapper(...textButton.$slots) {
    <expr>content</expr>()
}
