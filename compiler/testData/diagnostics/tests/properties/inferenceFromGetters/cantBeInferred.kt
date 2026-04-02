// RUN_PIPELINE_TILL: FRONTEND
// NI_EXPECTED_FILE
val x get() = <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>()
val y get() = <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>()

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, getter, nullableType, propertyDeclaration, typeParameter */
