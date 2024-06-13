// CHECK_TYPE
// WITH_EXTENDED_CHECKERS

package a

import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.Iterator<!>
import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<!> as Comp

import checkSubtype

fun bar(any: Any): <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Iterable<Int><!>? {
    val a: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<String><!>? = null
    val b: Iterable<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!>>
    val c : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<String><!>? = null

    if (any is <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>) {
        checkSubtype<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>>(<!DEBUG_INFO_SMARTCAST!>any<!>)
    }
    any as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>
    return null
}
