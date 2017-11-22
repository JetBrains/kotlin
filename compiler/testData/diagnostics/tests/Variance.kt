// !WITH_NEW_INFERENCE
package variance

abstract class Consumer<in T> {}

abstract class Producer<out T> {}

abstract class Usual<T> {}

fun foo(c: Consumer<Int>, p: Producer<Int>, u: Usual<Int>) {
    val c1: Consumer<Any> = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>c<!>
    val <!UNUSED_VARIABLE!>c2<!>: Consumer<Int> = c1

    val p1: Producer<Any> = p
    val <!UNUSED_VARIABLE!>p2<!>: Producer<Int> = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>p1<!>

    val u1: Usual<Any> = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>u<!>
    val <!UNUSED_VARIABLE!>u2<!>: Usual<Int> = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>u1<!>
}

//Arrays copy example
class Array<T>(val length : Int, val t : T) {
    fun get(<!UNUSED_PARAMETER!>index<!> : Int) : T { return t }
    fun set(<!UNUSED_PARAMETER!>index<!> : Int, <!UNUSED_PARAMETER!>value<!> : T) { /* ... */ }
}

fun copy1(<!UNUSED_PARAMETER!>from<!> : Array<Any>, <!UNUSED_PARAMETER!>to<!> : Array<Any>) {}

fun copy2(<!UNUSED_PARAMETER!>from<!> : Array<out Any>, <!UNUSED_PARAMETER!>to<!> : Array<in Any>) {}

fun <T> copy3(<!UNUSED_PARAMETER!>from<!> : Array<out T>, <!UNUSED_PARAMETER!>to<!> : Array<in T>) {}

fun copy4(<!UNUSED_PARAMETER!>from<!> : Array<out Number>, <!UNUSED_PARAMETER!>to<!> : Array<in Int>) {}

fun f(ints: Array<Int>, any: Array<Any>, numbers: Array<Number>) {
    copy1(<!TYPE_MISMATCH!>ints<!>, any)
    copy2(ints, any) //ok
    copy2(ints, <!TYPE_MISMATCH!>numbers<!>)
    copy3<Int>(ints, numbers)
    copy4(ints, numbers) //ok
}