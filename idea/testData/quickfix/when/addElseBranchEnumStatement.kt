// "Add else branch" "true"
enum class Color { R, G, B }
fun use(c: Color) {
    <caret>when (c) {
        Color.R -> red()
    }
}

fun red() {}
/* IGNORE_FIR */
