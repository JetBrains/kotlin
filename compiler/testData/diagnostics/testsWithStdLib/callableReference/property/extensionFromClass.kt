// !DIAGNOSTICS:-UNUSED_VARIABLE

import kotlin.reflect.*

class A {
    fun test() {
        val fooRef: KExtensionProperty<A, String> = ::foo
        val barRef: KMutableExtensionProperty<A, Int> = ::bar
    }
}

val A.foo: String get() = ""
var A.bar: Int
    get() = 42
    set(value) { }
