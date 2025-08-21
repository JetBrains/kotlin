// RUN_PIPELINE_TILL: BACKEND
class A {
    fun bar() {}
}

infix fun (() -> Unit).foo(x: A.() -> Unit) {}

fun main() {
    {
        return@foo
    } foo {
        bar()
        return@foo
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
lambdaLiteral, typeWithExtension */
