//KT-2606 Filter java.util.* import
package n

import java.util.*
import java.lang.annotation.*

fun bar() : Iterator<Int>? {
    val <!UNUSED_VARIABLE!>i<!> : Iterable<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!>>
    val <!UNUSED_VARIABLE!>a<!> : Annotation
    return null
}