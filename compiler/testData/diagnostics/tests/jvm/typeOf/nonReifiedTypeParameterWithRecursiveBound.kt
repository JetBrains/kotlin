// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: -JvmSupportRecursiveTypeOf

import kotlin.reflect.typeOf

fun <T : Comparable<T>> foo() {
    <!TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND!>typeOf<List<T>>()<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, typeConstraint, typeParameter */
