class B(val s: String)

class A {
    val b = B("zzz")
}

fun foo(): B {
    val a = A()
    return a.b
}

fun main(args: Array<String>) {
    println(foo().s)
}