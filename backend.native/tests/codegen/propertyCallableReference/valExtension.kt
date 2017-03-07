class A(y: Int) {
    var x = y
}

val A.z get() = this.x

fun main(args: Array<String>) {
    val p1 = A::z
    println(p1.get(A(42)))
    val a = A(117)
    val p2 = a::z
    println(p2.get())
}