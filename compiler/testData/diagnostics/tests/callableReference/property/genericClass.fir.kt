// !CHECK_TYPE

import kotlin.reflect.KProperty1

class A<T>(val t: T) {
    val foo: T = t
}

fun bar() {
    val x = A<String>::foo
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KProperty1<A<String>, String>>(x)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KProperty1<A<String>, Any?>>(x)

    val y = A<*>::foo
    checkSubtype<KProperty1<A<*>, Any?>>(y)
}
