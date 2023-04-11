// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND: JS_IR

interface Canvas {
    val suffix: String
}

interface Shape {
    context(Canvas)
    fun draw(): String
}

class Circle : Shape {
    context(Canvas)
    override fun draw() = "OK" + suffix
}

object MyCanvas : Canvas {
    override val suffix = ""
}

fun box() = with(MyCanvas) { Circle().draw() }
