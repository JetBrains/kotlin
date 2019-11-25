// IGNORE_BACKEND_FIR: JVM_IR
class A() {

    fun ok() = Foo.Bar.bar() + Foo.Bar.barv

    private object Foo {
        fun foo() = "O"
        val foov = "K"

        public object Bar {
            fun bar() = foo()
            val barv = foov
        }
    }
}

fun box() = A().ok()
