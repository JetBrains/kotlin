// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class TextPack(
    val color: String = "blue",
    val fontSize: Int = 12,
)

fun render(text: String = "title", ...TextPack.$props) {}

fun headline(...render.$props) {
    text.length
    color.length
    fontSize.inc()
}

fun card(...headline.$props) {
    text.length
    color.length
    fontSize.inc()
}

fun use() {
    headline<!NO_VALUE_FOR_PARAMETER!>()<!>
    headline(text = "title", color = "blue", fontSize = 12)
    headline(text = "title", color = "blue", fontSize = 18)
    card<!NO_VALUE_FOR_PARAMETER!>()<!>
    card(text = "title", color = "blue", fontSize = 12)
    card(text = "body", color = "red", fontSize = 12)
}
