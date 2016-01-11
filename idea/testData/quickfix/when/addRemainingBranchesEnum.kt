// "Add remaining branches" "true"
enum class Color { R, G, B }
fun test(c: Color) = wh<caret>en(c) {
    Color.B -> 0xff
}
