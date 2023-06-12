// WITH_STDLIB
// FILE: main.kt
import java.nio.charset.Charset

fun charset(charsetName: String): Charset = Charset.forName(charsetName)

fun test() = <expr>kotlin.text.charset("")</expr>