// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers
// FIR_IDENTICAL

interface Canvas

interface Shape {
    context(Canvas)
    fun draw(): Unit
}

class Circle : Shape {
    context(Canvas)
    override fun draw() {}
}

object MyCanvas : Canvas

fun test() = with(MyCanvas) { Circle().draw() }