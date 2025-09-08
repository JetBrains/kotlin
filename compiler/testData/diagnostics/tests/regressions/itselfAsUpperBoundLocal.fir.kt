// RUN_PIPELINE_TILL: FRONTEND
fun bar() {
    fun <<!CYCLIC_GENERIC_UPPER_BOUND!>T: T?<!>> foo() {}
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, localFunction, nullableType, typeConstraint, typeParameter */
