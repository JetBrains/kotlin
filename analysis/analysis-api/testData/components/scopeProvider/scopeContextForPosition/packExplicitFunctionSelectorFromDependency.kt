// MODULE: lib
// FILE: lib.kt
package lib

fun Text(text: String, color: String) {}

fun Text(value: Int, color: String) {}

val textString: (text: String, color: String) -> Unit = ::Text

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Text
import lib.textString

fun Wrapper(...Text.$props(textString)) {
    <expr>text</expr>
    color.length
}
