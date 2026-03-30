// MODULE: lib
// FILE: lib.kt
package lib

fun Text(text: String, color: String) {}

fun Text(value: Int, color: String) {}

val textString: (text: String, color: String) -> Unit = ::Text

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.textString

fun Wrapper(...textString.$props) {
    <expr>text</expr>
    color.length
}
