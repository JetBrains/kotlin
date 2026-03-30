// IGNORE_BACKEND_K1: ANY

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

fun card(...headline.$props): String {
    return headline(text = text, color = color, fontSize = fontSize)
}

fun box(): String {
    val fromHeadline = headline(text = "title", color = "blue", fontSize = 12)
    val fromHeadlineOverride = headline(text = "title", color = "blue", fontSize = 18)
    val fromCard = card(text = "title", color = "blue", fontSize = 12)
    val fromCardOverride = card(text = "body", color = "red", fontSize = 12)

    return if (
        fromHeadline == "title|blue|12" &&
        fromHeadlineOverride == "title|blue|18" &&
        fromCard == "title|blue|12" &&
        fromCardOverride == "body|red|12"
    ) {
        "OK"
    } else {
        "fail: $fromHeadline | $fromHeadlineOverride | $fromCard | $fromCardOverride"
    }
}
