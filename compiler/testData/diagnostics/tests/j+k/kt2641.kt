// !CHECK_TYPE

package a

import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.Iterator<!>
import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<!> as Comp

fun bar(any: Any): <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Iterable<Int><!>? {
    val <!UNUSED_VARIABLE!>a<!>: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<String><!>? = null
    val <!UNUSED_VARIABLE!>b<!>: Iterable<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!>>
    val <!UNUSED_VARIABLE!>c<!> : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<String><!>? = null

    if (any is <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>) {
        checkSubtype<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>>(<!DEBUG_INFO_SMARTCAST!>any<!>)
    }
    any as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Iterator<*><!>
    return null
}