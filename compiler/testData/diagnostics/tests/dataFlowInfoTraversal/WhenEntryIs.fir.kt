// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(x: Number, y: Int) {
    when (x) {
        is Int -> checkSubtype<Int>(x)
        y -> {}
        else -> {}
    }
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}

fun bar(x: Number) {
    when (x) {
        is Int -> checkSubtype<Int>(x)
        else -> {}
    }
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}

fun whenWithoutSubject(x: Number) {
    when {
        (x is Int) -> checkSubtype<Int>(x)
        else -> {}
    }
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, isExpression, nullableType, smartcast, typeParameter, typeWithExtension, whenExpression,
whenWithSubject */
