class A {
    fun f(flag: Boolean) {
        if (flag) {
            toString()
        }
        super.toString()
    }
}

fun main() {
    val a = A()
    a.f(false)
}
