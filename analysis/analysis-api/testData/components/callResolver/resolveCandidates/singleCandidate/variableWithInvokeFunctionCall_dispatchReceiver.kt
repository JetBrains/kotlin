operator fun Int.invoke() {}
class A {
    val f: Int = 1
    fun test() {
        <expr>f()</expr>
    }
}
