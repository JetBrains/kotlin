// RUN_PIPELINE_TILL: BACKEND
package test
fun f() {
    fun <A, B: A> f1() {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, localFunction, nullableType, typeConstraint, typeParameter */
