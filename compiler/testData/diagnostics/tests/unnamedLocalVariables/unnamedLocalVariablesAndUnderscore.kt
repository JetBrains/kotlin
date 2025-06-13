// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74809
// WITH_STDLIB
// LANGUAGE: +UnnamedLocalVariables, +ContextParameters

fun testLambdaUnderscore() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = { _: Int ->
        val <!UNDERSCORE_IS_RESERVED!>_<!>: Int = 1
    }
}

fun testAnonymousUnderscore() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = fun(_: Int) {
        val <!UNDERSCORE_IS_RESERVED!>_<!> = 1
    }
}

fun testTypeUnderscore() {
    val <!UNDERSCORE_IS_RESERVED!>_<!>: MutableList<String> = mutableListOf<_>("")
}

fun testUnderscoreInCatch() {
    try {
        val <!UNDERSCORE_IS_RESERVED!>_<!>: Exception = Exception()
    } catch (_: Exception) {
        val <!UNDERSCORE_IS_RESERVED!>_<!>: Exception = Exception()
    }
}

class A {
    fun foo() {}
}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(_: <!DEBUG_INFO_MISSING_UNRESOLVED!>A<!>)<!>
fun testContextParameters() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = ""
}

fun testLocalContext() {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(_: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun foo() {}

    val <!UNDERSCORE_IS_RESERVED!>_<!>: String = ""
}

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, functionDeclaration, functionDeclarationWithContext,
integerLiteral, lambdaLiteral, localFunction, localProperty, propertyDeclaration, stringLiteral, tryExpression,
unnamedLocalVariable */
