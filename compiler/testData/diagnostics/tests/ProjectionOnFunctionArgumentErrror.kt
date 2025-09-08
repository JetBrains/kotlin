// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun test() {
    fun <T> foo(){}
    foo<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>in<!> Int>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, localFunction, nullableType, typeParameter */
