// WITH_STDLIB

open class A

class B : A()

fun box(): String {
    val seq: Sequence<A> = sequenceOf(B(), B())
    seq.forEach({  })
    return "OK"
}
