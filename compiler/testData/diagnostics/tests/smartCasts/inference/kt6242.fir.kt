// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE

inline fun<T> foo(block: () -> T):T = block()

fun baz() {
    val x: String = foo {
        val task: String? = null
        if (task == null) {
            return
        } else task
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, inline, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast, typeParameter */
