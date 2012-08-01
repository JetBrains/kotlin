package a

import java.util.*

fun <T> emptyList() : List<T>? = ArrayList<T>()

fun foo() {
    // type arguments shouldn't be required
    val <!UNUSED_VARIABLE!>l<!> : List<Int> = emptyList()!!
    val <!UNUSED_VARIABLE!>l1<!> = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()!!

    emptyList()!! : List<Int>
    emptyList() : List<Int>?

    doWithList(emptyList()!!)
}

fun doWithList(list: List<Int>) = list