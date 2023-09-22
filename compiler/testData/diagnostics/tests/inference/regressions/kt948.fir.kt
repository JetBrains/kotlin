// !CHECK_TYPE

//KT-948 Make type inference work with sure()/!!

package a

import java.util.*
import checkSubtype

fun <T> emptyList() : List<T>? = ArrayList<T>()

fun foo() {
    // type arguments shouldn't be required
    val l : List<Int> = emptyList()!!
    val l1 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()!!<!>

    checkSubtype<List<Int>>(emptyList()!!)
    checkSubtype<List<Int>?>(emptyList())

    doWithList(emptyList()!!)
}

fun doWithList(list: List<Int>) = list
