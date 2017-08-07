class A(val s: String)

class B {
    var a: A? = null
}

class C(val b: B)

fun foo(c: C) {
    c.b.a = A("zzz")
}

fun bar(b: B) {
    val c = C(b)
    foo(c)
}

val global = B()

fun main(args: Array<String>) {
    bar(global)
    println(global.a!!.s)
}