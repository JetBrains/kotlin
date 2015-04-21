// !CHECK_TYPE

import kotlin.reflect.KMemberFunction0

class A<T>(val t: T) {
    fun foo(): T = t
}

fun bar() {
    val x = A<String>::foo

    checkSubtype<KMemberFunction0<A<String>, String>>(x)
}
