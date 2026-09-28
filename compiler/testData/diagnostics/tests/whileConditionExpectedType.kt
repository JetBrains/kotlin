// RUN_PIPELINE_TILL: CODEGEN
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
        materialize() -> {}
        else -> {}
    }
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, doWhileLoop, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, localProperty, nullableType, outProjection, propertyDeclaration, typeParameter, vararg,
whenExpression, whenWithSubject, whileLoop */
