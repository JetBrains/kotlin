// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

class Generic<T> {
    override fun equals(@EqualityBound(Generic::class) other: Any?) = true
}

fun local() {
    class Generic<T> {
        override fun equals(@EqualityBound(bound = Generic::class) other: Any?) = true
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, localClass, nullableType, operator,
override, typeParameter */
