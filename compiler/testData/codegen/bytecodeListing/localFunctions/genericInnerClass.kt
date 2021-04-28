// WITH_SIGNATURES

class A<T>(val result: T) {
    inner class B {
        inner class C {
            fun f() {
                fun g(t: T) {}
            }
        }
    }
}
