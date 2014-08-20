class A(val n: Int, val s: String, val o: Any) {
    fun component1(): Int = n
    fun component2(): String = s
    fun component3(): Any = o
}

fun test() {
    val a = A(1, "2", Any())
    a.n
    a.component1()
    val (x, y, z) = a
}