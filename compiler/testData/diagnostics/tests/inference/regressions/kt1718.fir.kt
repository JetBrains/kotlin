// !CHECK_TYPE

//KT-1718 compiler error when not using temporary variable
package n

import java.util.ArrayList
import checkSubtype

fun test() {
    val list = arrayList("foo", "bar") + arrayList("cheese", "wine")
    checkSubtype<List<String>>(list)
    //check it's not an error type
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>list<!>)
}

//from library
fun <T> arrayList(vararg values: T) : ArrayList<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun <T> Iterable<T>.plus(elements: Iterable<T>): List<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
