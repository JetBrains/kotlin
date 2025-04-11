// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74809
// WITH_STDLIB
// LANGUAGE: +UnnamedLocalVariables, +ContextParameters

fun testLambdaUnderscore() {
    val _ = { _: Int ->
        val _: Int = 1
    }
}

fun testAnonymousUnderscore() {
    val _ = fun(_: Int) {
        val _ = 1
    }
}

fun testTypeUnderscore() {
    val _: MutableList<String> = mutableListOf<_>("")
}

fun testUnderscoreInCatch() {
    try {
        val _: Exception = Exception()
    } catch (_: Exception) {
        val _: Exception = Exception()
    }
}

class A {
    fun foo() {}
}

context(_: A)
fun testContextParameters() {
    val _ = ""
}

fun testLocalContext() {
    context(_: String)
    fun foo() {}

    val _: String = ""
}