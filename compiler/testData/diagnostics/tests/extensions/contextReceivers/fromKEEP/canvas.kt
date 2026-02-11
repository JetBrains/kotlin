// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

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

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration,
lambdaLiteral, objectDeclaration, override */
