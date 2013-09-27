package s

trait In<in T>

trait A
trait B
trait C: A, B

fun <T> foo(in1: In<T>, in2: In<T>): T = throw Exception("$in1 $in2")

fun test(inA: In<A>, inB: In<B>, inC: In<C>) {

    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>foo<!>(inA, inB)

    val r = foo(inA, inC)
    r: C

    val c: C = foo(inA, inB)

    use(c)
}

fun <T: C> bar(in1: In<T>): T = throw Exception("$in1")

fun test(inA: In<A>) {
    val r = bar(inA)
    r: C
}

fun use(vararg a: Any?) = a