// IGNORE_FIR
// KT-55626
class Bar(val a: Int) {
    val b: Int = { 0 }()

    fun getAPlusB() = a + b

    class Baz {
        fun doSomething() {

        }
    }
}
