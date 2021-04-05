package variance

abstract class Consumer<in T> {}

abstract class Producer<out T> {}

abstract class Usual<T> {}

fun foo(c: Consumer<Int>, p: Producer<Int>, u: Usual<Int>) {
    val c1: Consumer<Any> = c
    val c2: Consumer<Int> = c1

    val p1: Producer<Any> = p
    val p2: Producer<Int> = p1

    val u1: Usual<Any> = u
    val u2: Usual<Int> = u1
}

//Arrays copy example
class Array<T>(val length : Int, val t : T) {
    fun get(index : Int) : T { return t }
    fun set(index : Int, value : T) { /* ... */ }
}

fun copy1(from : Array<Any>, to : Array<Any>) {}

fun copy2(from : Array<out Any>, to : Array<in Any>) {}

fun <T> copy3(from : Array<out T>, to : Array<in T>) {}

fun copy4(from : Array<out Number>, to : Array<in Int>) {}

fun f(ints: Array<Int>, any: Array<Any>, numbers: Array<Number>) {
    copy1(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is variance/Array<kotlin/Int> but variance/Array<kotlin/Any> was expected">ints</error>, any)
    copy2(ints, any) //ok
    copy2(ints, <error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is variance/Array<kotlin/Number> but variance/Array<in kotlin/Any> was expected">numbers</error>)
    copy3<Int>(ints, numbers)
    copy4(ints, numbers) //ok
}
