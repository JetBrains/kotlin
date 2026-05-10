// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86144

class A

operator fun <T> T.plus(x: () -> T) = A()
operator fun <T> T.plusAssign(x: () -> T) {}

fun <T> id(x: T) = x

fun testAssignment() {
    var value = A()
    value <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> id {
        <!UNRESOLVED_REFERENCE!>unresolved<!>()
        A()
    }
}

fun testIndexed() {
    val array = emptyArray<A>()
    array[0] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> id {
        <!UNRESOLVED_REFERENCE!>unresolved<!>()
        A()
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, lambdaLiteral, localProperty, nullableType, operator, propertyDeclaration, typeParameter */
