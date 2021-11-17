enum class Color {
    R,
    G,
    B
}

annotation class Annotation(val color : Color)

@Annotation(<expr>Color.R</expr>)
class C
