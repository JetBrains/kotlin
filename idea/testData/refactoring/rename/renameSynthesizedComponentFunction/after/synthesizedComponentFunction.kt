data class A(val foo: Int, val s: String, val o: Any)

fun test() {
    val a = A(1, "2", Any())
    a.foo
    a.component1()
    val (x, y, z) = a
}