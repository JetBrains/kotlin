// !CHECK_TYPE

import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<!> as Comparable

fun f(c: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Comparable<*><!>) {
    checkSubtype<kotlin.Comparable<*>>(<!TYPE_MISMATCH!>c<!>)
    checkSubtype<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<*><!>>(c)
}