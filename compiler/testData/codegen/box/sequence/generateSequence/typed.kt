// WITH_STDLIB

open class A

class B : A()

fun box(): String {
    val seq: Sequence<A> = generateSequence(B()) { null }
    seq.forEach({  })
    return "OK"
}
