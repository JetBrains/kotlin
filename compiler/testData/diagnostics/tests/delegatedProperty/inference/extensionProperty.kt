package foo

open class A {
    val B.w: Int by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyProperty<!>()
}

val B.r: Int by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyProperty<!>()

val A.e: Int by MyProperty()

class B {
    val A.f: Int by MyProperty()
}

class MyProperty<R : A, T> {
    public fun get(thisRef: R, desc: PropertyMetadata): T {
        throw Exception("$thisRef $desc")
    }
}
