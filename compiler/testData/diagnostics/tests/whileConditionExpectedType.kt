// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-58674
// INFERENCE_HELPERS

fun test() {
    while (materialize()) {

    }

    do {

    } while (materialize())

    if (materialize()) {

    }

    when (val it = materialize<Boolean>()) {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>() -> {}
        else -> {}
    }
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, doWhileLoop, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, localProperty, nullableType, outProjection, propertyDeclaration, typeParameter, vararg,
whenExpression, whenWithSubject, whileLoop */
