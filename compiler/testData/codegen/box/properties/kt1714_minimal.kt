interface A {
    val v: Int
}

class AImpl : A {
    override val v: Int = 5
}

fun box() : String {
    val a: A = AImpl()
    a.v
    return "OK"
}
