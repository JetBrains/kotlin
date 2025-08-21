// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
interface A
interface X: A?<!NULLABLE_SUPERTYPE, REDUNDANT_NULLABLE!>?<!> {

}

fun <T> interaction(t: T) {
    if (t == null) {}

}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, nullableType,
typeParameter */
