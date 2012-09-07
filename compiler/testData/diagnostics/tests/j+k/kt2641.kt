//KT-2641 Warn on using j.l.Iterable in Kotlin code
package n

import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.Iterator<!>
import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<!> as Comp

fun bar() : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Iterable<Int><!>? {
    val <!UNUSED_VARIABLE!>a<!> : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<String><!>? = null
    val <!UNUSED_VARIABLE!>b<!> : Iterable<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!>>
    return null
}