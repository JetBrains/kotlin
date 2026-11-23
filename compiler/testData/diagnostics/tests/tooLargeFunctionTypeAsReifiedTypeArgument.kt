// RUN_PIPELINE_TILL: FRONTEND

inline fun <reified T> foo() {}

fun bar() {
    foo<
        <!FUNCTION_TYPE_OF_TOO_LARGE_ARITY!>(
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
            Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        ) -> Unit<!>,
    >()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, nullableType, reified, typeParameter */
