// CHECK_TYPE
// WITH_EXTENDED_CHECKERS

package a

import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.Iterator<!>
import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<!> as Comp

import checkSubtype

fun bar(any: Any): <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Iterable<Int><!>? {
    val <!UNUSED_VARIABLE!>a<!>: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<String><!>? = null
    val <!UNUSED_VARIABLE!>b<!>: Iterable<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!>>
    val <!UNUSED_VARIABLE!>c<!> : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<String><!>? = null

    if (any is <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>) {
        checkSubtype<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>>(any)
    }
    any as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>
    return null
}
