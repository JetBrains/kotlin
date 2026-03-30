// IGNORE_BACKEND_K1: ANY

// MODULE: lib
// FILE: lib.kt
package lib

fun Text(text: String, color: String, modifier: Int = 0): String {
    return "string:$text/$color/$modifier"
}

fun Text(value: Int, color: String, modifier: Int = 0): String {
    return "int:$value/$color/$modifier"
}

val textString: (text: String, color: String, modifier: Int) -> String = ::Text

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Text
import lib.textString

fun SharedWrapper(...Text.$sharedProps): String {
    return "shared:$color/$modifier"
}

fun ExplicitWrapper(...Text.$props(textString)): String {
    return Text(text = text, color = color, modifier = modifier)
}

fun box(): String {
    val shared = SharedWrapper(color = "red", modifier = 1)
    val explicit = ExplicitWrapper(text = "hello", color = "blue", modifier = 2)

    return if (
        shared == "shared:red/1" &&
        explicit == "string:hello/blue/2"
    ) {
        "OK"
    } else {
        "fail: $shared | $explicit"
    }
}
