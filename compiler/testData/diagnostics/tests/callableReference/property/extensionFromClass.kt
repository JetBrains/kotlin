// !DIAGNOSTICS:-UNUSED_VARIABLE

import kotlin.reflect.*

class A {
    fun test() {
        val fooRef: KProperty1<A, String> = ::foo
        val barRef: KMutableProperty1<A, Int> = ::bar
    }
}

val A.foo: String get() = ""
var A.bar: Int
    get() = 42
    set(value) { }
