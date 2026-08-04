// WITH_STDLIB

open class A

class B : A()

fun test(seq: Sequence<A>): String {
    seq.forEach({  })
    return "OK"
}

fun box(): String {
    val seq = sequenceOf(B(), B())
    return test(seq)
}
