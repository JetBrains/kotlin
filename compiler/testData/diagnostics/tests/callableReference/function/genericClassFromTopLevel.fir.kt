// !CHECK_TYPE

import kotlin.reflect.KFunction1

class A<T>(val t: T) {
    fun foo(): T = t
}

fun bar() {
    val x = A<String>::foo

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KFunction1<A<String>, String>>(x)
}
