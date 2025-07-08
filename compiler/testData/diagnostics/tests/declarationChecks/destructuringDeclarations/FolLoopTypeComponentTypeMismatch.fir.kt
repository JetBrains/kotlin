// RUN_PIPELINE_TILL: FRONTEND

class A {
    operator fun component1() = 1
    operator fun component2() = 1.0
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x: Double, y: Int) in <!COMPONENT_OPERATOR_RETURN_TYPE_MISMATCH, COMPONENT_OPERATOR_RETURN_TYPE_MISMATCH!>C()<!>) {

    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, forLoop, functionDeclaration, integerLiteral, localProperty,
operator, propertyDeclaration */
