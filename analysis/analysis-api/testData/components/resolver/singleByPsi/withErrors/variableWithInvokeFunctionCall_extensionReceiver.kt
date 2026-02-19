operator fun Int.invoke() {}
class A {
    fun test() {
        <expr>f()</expr>
    }
}

val A.f: Int = 1
