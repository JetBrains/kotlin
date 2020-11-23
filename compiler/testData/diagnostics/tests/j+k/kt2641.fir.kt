// !CHECK_TYPE

package a

import java.util.Iterator
import java.lang.Comparable as Comp

import checkSubtype

fun bar(any: Any): java.lang.Iterable<Int>? {
    val a: java.lang.Comparable<String>? = null
    val b: Iterable<Integer>
    val c : Iterator<String>? = null

    if (any is Iterator<*>) {
        checkSubtype<Iterator<*>>(any)
    }
    any as Iterator<*>
    return null
}
