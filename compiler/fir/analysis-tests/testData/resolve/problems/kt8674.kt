// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-8674

// KT-8674: Call resolution — basic overload resolution and call convention scenarios

fun foo(x: Int): String = x.toString()
fun foo(x: String): String = x

fun baz(): Int = 42

interface Runnable {
    fun run()
}

fun acceptRunnable(r: Runnable) {}

fun main() {
    val a: String = foo(1)
    val b: String = foo("hello")

    val c: Int = baz()

    acceptRunnable(object : Runnable {
        override fun run() {}
    })
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, integerLiteral, interfaceDeclaration,
localProperty, override, propertyDeclaration, stringLiteral */
