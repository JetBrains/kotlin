//KT-2641 Warn on using j.l.Iterable in Kotlin code
package n

import <!CLASS_HAS_KOTLIN_ANALOG!>java.util.Iterator<!>
import <!CLASS_HAS_KOTLIN_ANALOG!>java.lang.Comparable<!> as Comp

fun bar() : <!CLASS_HAS_KOTLIN_ANALOG!>java.lang.Iterable<Int><!>? {
    val <!UNUSED_VARIABLE!>a<!> : <!CLASS_HAS_KOTLIN_ANALOG!>java.lang.Comparable<String><!>? = null
    val <!UNUSED_VARIABLE!>b<!> : Iterable<<!CLASS_HAS_KOTLIN_ANALOG!>Integer<!>>
    return null
}