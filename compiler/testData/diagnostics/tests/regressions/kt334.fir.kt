// !CHECK_TYPE

import java.lang.Comparable as Comparable

fun f(c: Comparable<*>) {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><kotlin.Comparable<*>>(c)
    checkSubtype<java.lang.Comparable<*>>(c)
}