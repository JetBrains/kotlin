enum class Color {
    R,
    G,
    B
}

annotation class Annotation(val color : Color)

<expr>@Annotation(Color.R)</expr>
class C
