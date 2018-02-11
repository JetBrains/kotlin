// !WITH_NEW_INFERENCE
// !CHECK_TYPE

//KT-948 Make type inference work with sure()/!!

package a

import java.util.*

fun <T> emptyList() : List<T>? = ArrayList<T>()

fun foo() {
    // type arguments shouldn't be required
    val <!UNUSED_VARIABLE!>l<!> : List<Int> = emptyList()!!
    <!NI;UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>l1<!> =<!> <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()!!

    <!NI;UNREACHABLE_CODE!>checkSubtype<List<Int>>(emptyList()!!)<!>
    <!NI;UNREACHABLE_CODE!>checkSubtype<List<Int>?>(emptyList())<!>

    <!NI;UNREACHABLE_CODE!>doWithList(emptyList()!!)<!>
}

fun doWithList(list: List<Int>) = list