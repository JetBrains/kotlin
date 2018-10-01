// "Add else branch" "true"
// WITH_RUNTIME
enum class Color { R, G, B }
fun use(c: Color) {
    <caret>when (c) {
        Color.R -> red()
    }
}

fun red() {}
