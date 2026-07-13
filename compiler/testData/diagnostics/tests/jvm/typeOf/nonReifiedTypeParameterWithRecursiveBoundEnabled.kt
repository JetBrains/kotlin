// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +JvmSupportRecursiveTypeOf

import kotlin.reflect.typeOf

fun <T : Comparable<T>> foo() {
    typeOf<List<T>>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, typeConstraint, typeParameter */
