// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

typealias A<T> = Array<T>

fun foo(p: Int) {
    val a = arrayOf(1, 2, 3)
    val b = arrayOf(3, 2, 1)
    val c : A<Int> = arrayOf(3, 2, 1)

    if (a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> b) { }
    if (a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> c) { }
}

fun testsFromIdea() {
    val a = arrayOf("a")
    val b = a
    val c: Any? = null
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> b
    a == c
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>!=<!> b
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
