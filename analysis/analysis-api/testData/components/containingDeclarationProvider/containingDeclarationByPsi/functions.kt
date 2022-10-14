fun a(){}
private fun b() {}

class A {
    private fun a() {}

    class B {
        private fun b() {}

        class C {
            private fun c() {}

        }
    }
}

interface D {
    private fun d() {}
    inner class E {
        private fun e() {}
        enum class F {
            private fun f() {}
        }
    }
}

enum class G {
    H, I;

    private fun g() {}
}


fun foo() {
    private fun fooA() {}

    class FooI {
        private fun m() {}
    }
}