operator fun Int.invoke(i: Int) {}

class A(val x: Int) {
    fun x(b: Boolean) {}
}

fun call(a: A) {
    <expr>a.x(1)</expr>
}