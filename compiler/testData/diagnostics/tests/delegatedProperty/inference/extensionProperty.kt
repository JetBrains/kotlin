// !WITH_NEW_INFERENCE
package foo

import kotlin.reflect.KProperty

open class A {
    val B.w: Int by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>MyProperty<!>()<!>
}

val B.r: Int by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>MyProperty<!>()<!>

val A.e: Int by MyProperty()

class B {
    val A.f: Int by MyProperty()
}

class MyProperty<R : A, T> {
    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        throw Exception("$thisRef $desc")
    }
}
