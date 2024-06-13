// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -UNUSED_VARIABLE

import kotlin.reflect.KProperty1

class Scope {
    abstract class Nested<T> {
        abstract val key: Int
        abstract val keyT: T
    }
}

fun simple(a: Any?) {}
fun <K> id(x: K): K = x

fun test() {
    simple(Scope.Nested<String>::key)
    val a = id(Scope.Nested<String>::keyT)

    a

    val b = id(Scope.Nested<*>::keyT)

    b

    val c = id(Scope.Nested<out Number?>::keyT)

    c

    val d = id(Scope.Nested<*>::keyT <!UNCHECKED_CAST!>as Scope.Nested<Number><!>)

    d

    val g = id<KProperty1<Scope.Nested<*>, Any?>>(Scope.Nested<*>::keyT)

    g
}

fun justResolve() {
    val a = Scope.Nested<String>::key
    val b = Scope.Nested<String>::keyT
    val c = Scope.Nested<*>::keyT
    val d = Scope.Nested<out Number?>::keyT
}
