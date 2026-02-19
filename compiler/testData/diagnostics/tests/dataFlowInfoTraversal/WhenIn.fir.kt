// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(x: Int, list: List<Int>?) {
    when (x) {
        in list!! -> checkSubtype<List<Int>>(list)
        else -> {}
    }
}

fun whenWithoutSubject(x: Int, list: List<Int>?) {
    when {
        x in list!! -> checkSubtype<List<Int>>(list)
        else -> {}
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, nullableType, smartcast, typeParameter, typeWithExtension, whenExpression, whenWithSubject */
