// RUN_PIPELINE_TILL: FRONTEND
fun test(<!REDECLARATION, REDECLARATION!>a<!>: Int, <!REDECLARATION, REDECLARATION!>a<!>: String) {}

fun test2(block: (Int, String) -> Unit) { }

fun main() {
    test2 { <!REDECLARATION, REDECLARATION!>b<!>, <!REDECLARATION, REDECLARATION!>b<!> -> ; }

    val func: (Int, Int) -> Int = fun(_, _): Int { return 42 }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration */
