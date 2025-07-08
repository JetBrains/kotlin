// RUN_PIPELINE_TILL: FRONTEND

class A {
    operator fun component1() : Int = 1
    operator fun component2() : Int = 2
}

fun a(aa : A) {
    val (a: String, b1: String) = <!COMPONENT_OPERATOR_RETURN_TYPE_MISMATCH, COMPONENT_OPERATOR_RETURN_TYPE_MISMATCH!>aa<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, functionDeclaration, integerLiteral, localProperty,
operator, propertyDeclaration */
