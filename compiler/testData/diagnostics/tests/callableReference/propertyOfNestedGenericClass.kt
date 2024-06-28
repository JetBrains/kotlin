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

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty1<Scope.Nested<kotlin.String>, kotlin.String>")!>a<!>

    val b = id(Scope.Nested<*>::keyT)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty1<Scope.Nested<*>, kotlin.Any?>")!>b<!>

    val c = id(Scope.Nested<out Number?>::keyT)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty1<Scope.Nested<out kotlin.Number?>, kotlin.Number?>")!>c<!>

    val d = id(Scope.Nested<*>::keyT <!UNCHECKED_CAST!>as Scope.Nested<Number><!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("Scope.Nested<kotlin.Number>")!>d<!>

    val g = id<KProperty1<Scope.Nested<*>, Any?>>(Scope.Nested<*>::keyT)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty1<Scope.Nested<*>, kotlin.Any?>")!>g<!>
}

fun justResolve() {
    val a = Scope.Nested<String>::key
    val b = Scope.Nested<String>::keyT
    val c = Scope.Nested<*>::keyT
    val d = Scope.Nested<out Number?>::keyT
}