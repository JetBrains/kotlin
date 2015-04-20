// !CHECK_TYPE

import kotlin.reflect.KMemberProperty

class A<T>(val t: T) {
    val foo: T = t
}

fun bar() {
    val x = A<String>::foo
    checkSubtype<KMemberProperty<A<String>, String>>(x)
    checkSubtype<KMemberProperty<A<String>, Any?>>(x)

    val y = A<*>::foo
    checkSubtype<KMemberProperty<A<*>, Any?>>(y)
}
