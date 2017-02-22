interface A {
    val x: Int
}

class C: A {
    override val x: Int = 42
}

class Q(a: A): A by a

fun box(): String {
    val q = Q(C())
    val a: A = q
    return q.x.toString() + a.x.toString()
}

fun main(args: Array<String>) {
    println(box())
}