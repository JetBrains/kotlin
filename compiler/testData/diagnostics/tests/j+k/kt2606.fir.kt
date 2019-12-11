//KT-2606 Filter java.util.* import
package n

import java.util.*
import java.lang.annotation.*

fun bar() : Iterator<Int>? {
    val i : Iterable<Integer>
    val a : Annotation
    return null
}