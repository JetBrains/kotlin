// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// LANGUAGE: -JvmSupportRecursiveTypeOf

import kotlin.reflect.typeOf

fun <T : Comparable<T>> foo() {
    typeOf<List<T>>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, typeConstraint, typeParameter */
