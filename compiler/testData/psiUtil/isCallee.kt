package aa

annotation class A(val b: B)
annotation class B
annotation class C
open class D<T>(a: Any) {
    open fun test() {}
}
open class E
class F
interface G

@aa./*true*/A(/*true*/B())
@C
class I<T>: /*true*/D<T>(/*true*/E()) {
    override fun test() {
        /*false*/super./*true*/test()
        /*false*/this@I()
    }

    operator fun invoke() {}
}

class K constructor(): /*false*/G {
    constructor(i: Int): this()
}

class L: /*false*/E {
    constructor(i: Int): super()
}

fun a() = 12
fun Int.b() = 12
infix fun Int.c(i: Int) = 12

fun foo(a: Int = /*true*/a()) {
    12./*true*/b()
    /*false*/aa./*true*/a()
    12 /*true*/c 12
    var b = 12
    /*false*/b++
}

fun ((i: Int) -> Int).f() {
    /*false*/this(12)
}