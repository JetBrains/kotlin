// WITH_RUNTIME
// IS_APPLICABLE: false

interface I {
    val foo: Int
}

object Test : I {
    override val <caret>foo = 1
}