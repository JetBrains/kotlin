// !CHECK_TYPE

import java.lang.Comparable as Comparable

fun f(c: Comparable<*>) {
    checkSubtype<kotlin.Comparable<*>>(<!ARGUMENT_TYPE_MISMATCH!>c<!>)
    checkSubtype<java.lang.Comparable<*>>(c)
}