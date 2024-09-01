// FIR_IDENTICAL
// WITH_STDLIB

class Foo
class Bar {
    fun barFunction() = println("I am Bar")
}

fun main(map: Map<Foo, Bar>) {
    takeMap(map.mapValues { it.value }) { it.barFunction() }
    takeMap(doSomething = { it.<!UNRESOLVED_REFERENCE!>barFunction<!>() }, map = map.mapValues { it.value })
    takeMap2({ it.<!UNRESOLVED_REFERENCE!>barFunction<!>() }, map.mapValues { it.value })
}

fun <T : Any> takeMap(map: Map<Foo, T>, doSomething: (T) -> Unit) {}
fun <T : Any> takeMap2(doSomething: (T) -> Unit, map: Map<Foo, T>) {}