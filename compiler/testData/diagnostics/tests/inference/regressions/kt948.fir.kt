// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

//KT-948 Make type inference work with sure()/!!

package a

import java.util.*
import checkSubtype

fun <T> emptyList() : List<T>? = ArrayList<T>()

fun foo() {
    // type arguments shouldn't be required
    val l : List<Int> = emptyList()!!
    val l1 = <!CANNOT_INFER_PARAMETER_TYPE!>emptyList<!>()!!

    checkSubtype<List<Int>>(emptyList()!!)
    checkSubtype<List<Int>?>(emptyList())

    doWithList(emptyList()!!)
}

fun doWithList(list: List<Int>) = list

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, javaFunction, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
