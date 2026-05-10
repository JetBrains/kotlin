// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals +ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases

class A {
    companion object {
        operator fun of(vararg x: () -> Unit): A = A()
        inline operator fun of(a: () -> Unit, noinline b: () -> Unit, crossinline c: () -> Unit): A = A()
    }
}

fun test() {
    val a: A = [{ <!RETURN_NOT_ALLOWED!>return<!> }]
    val b: A = [{ return }, { <!RETURN_NOT_ALLOWED!>return<!> }, { <!RETURN_NOT_ALLOWED!>return<!> }]

    while (true) {
        val a: A = [{ <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }]
        val b: A = [{ continue }, { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }, { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }]
    }
}

fun take(a: A) { }

fun test2() = take([{ <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> }, { }, { }])
fun test3(): Unit = take([{ return }, { }, { }])

/* GENERATED_FIR_TAGS: break, classDeclaration, companionObject, continue, crossinline, functionDeclaration,
functionalType, inline, lambdaLiteral, localProperty, noinline, objectDeclaration, operator, propertyDeclaration, vararg,
whileLoop */
