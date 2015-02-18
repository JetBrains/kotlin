package variance

abstract class Consumer<in T> {}

abstract class Producer<out T> {}

abstract class Usual<T> {}

fun foo(c: Consumer<Int>, p: Producer<Int>, u: Usual<Int>) {
    val c1: Consumer<Any> = <error>c</error>
    val <warning>c2</warning>: Consumer<Int> = c1

    val p1: Producer<Any> = p
    val <warning>p2</warning>: Producer<Int> = <error>p1</error>

    val u1: Usual<Any> = <error>u</error>
    val <warning>u2</warning>: Usual<Int> = <error>u1</error>
}

//Arrays copy example
class Array<T>(val length : Int, val t : T) {
    fun get(index : Int) : T { return t }
    fun set(index : Int, value : T) { /* ... */ }
}

fun copy1(<warning>from</warning> : Array<Any>, <warning>to</warning> : Array<Any>) {}

fun copy2(<warning>from</warning> : Array<out Any>, <warning>to</warning> : Array<in Any>) {}

fun <T> copy3(<warning>from</warning> : Array<out T>, <warning>to</warning> : Array<in T>) {}

fun copy4(<warning>from</warning> : Array<out Number>, <warning>to</warning> : Array<in Int>) {}

fun f(ints: Array<Int>, any: Array<Any>, numbers: Array<Number>) {
    copy1(<error>ints</error>, any)
    copy2(ints, any) //ok
    copy2(ints, <error>numbers</error>)
    copy3<Int>(ints, numbers)
    copy4(ints, numbers) //ok
}
