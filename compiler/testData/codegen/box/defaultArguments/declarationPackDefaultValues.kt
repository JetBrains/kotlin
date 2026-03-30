// IGNORE_BACKEND_K1: ANY

data class TextPack(
    val color: String = "blue",
    val fontSize: Int = 12,
)

open class DirectDefaults {
    val label: String = "fallback"
    val enabled: Boolean = true
}

fun addText(text: String, ...TextPack.$props): String {
    return "$text|$color|$fontSize"
}

fun titleText(...addText.$props): String {
    return addText(text = text, color = color, fontSize = fontSize)
}

fun renderDirect(...DirectDefaults.$props): String {
    return "$label|$enabled"
}

fun box(): String {
    val fromTypePack = addText(text = "hello")
    val fromFunctionPack = titleText(text = "world", color = "blue", fontSize = 12)
    val withOverride = titleText(text = "override", color = "blue", fontSize = 18)
    val fromInitializer = renderDirect()

    return if (
        fromTypePack == "hello|blue|12" &&
        fromFunctionPack == "world|blue|12" &&
        withOverride == "override|blue|18" &&
        fromInitializer == "fallback|true"
    ) {
        "OK"
    } else {
        "fail: $fromTypePack | $fromFunctionPack | $withOverride | $fromInitializer"
    }
}
