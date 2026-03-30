// IGNORE_BACKEND_K1: ANY

// MODULE: lib
// FILE: lib.kt
package lib

class TextPack(
    val color: String = "blue",
    val fontSize: Int = 12,
)

fun render(text: String = "title", ...TextPack.$props): String {
    return "$text|$color|$fontSize"
}

fun headline(...render.$props): String {
    return render(text = text, color = color, fontSize = fontSize)
}

// MODULE: main(lib)
// FILE: main.kt
import lib.headline

fun box(): String {
    val fromDefaults = headline(text = "title", color = "blue", fontSize = 12)
    val fromOverride = headline(text = "title", color = "red", fontSize = 12)

    return if (
        fromDefaults == "title|blue|12" &&
        fromOverride == "title|red|12"
    ) {
        "OK"
    } else {
        "fail: $fromDefaults | $fromOverride"
    }
}
