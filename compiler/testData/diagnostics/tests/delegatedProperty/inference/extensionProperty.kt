package foo

import kotlin.reflect.KProperty

open class A {
    val B.w: Int by <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>MyProperty<!>()
}

val B.r: Int by <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>MyProperty<!>()

val A.e: Int by MyProperty()

class B {
    val A.f: Int by MyProperty()
}

class MyProperty<R : A, T> {
    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        throw Exception("$thisRef $desc")
    }
}
