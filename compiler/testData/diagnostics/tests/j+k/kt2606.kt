//KT-2606 Filter java.util.* import
package n

import java.util.*
import java.lang.annotation.*

fun bar() : Iterator<Int>? {
    val <!UNUSED_VARIABLE!>i<!> : Iterable<<!CLASS_HAS_KOTLIN_ANALOG!>Integer<!>>
    val <!UNUSED_VARIABLE!>a<!> : Annotation
    return null
}