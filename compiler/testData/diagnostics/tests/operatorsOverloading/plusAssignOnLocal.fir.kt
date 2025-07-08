// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class C

operator fun C.plus(a: Any): C = this
operator fun C.plusAssign(a: Any) {}

fun test() {
    val c = C()
    c += ""
    var c1 = C()
    c1 <!ASSIGNMENT_OPERATOR_AMBIGUITY!>+=<!> ""
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localProperty, operator,
propertyDeclaration, stringLiteral, thisExpression */
