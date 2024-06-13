// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_EXTENDED_CHECKERS
//KT-2606 Filter java.util.* import
package n

import java.util.*
import java.lang.annotation.*

fun bar() : Iterator<Int>? {
    val i : Iterable<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!>>
    val a : Annotation
    return null
}
