// See KT-62714
// IGNORE_BACKEND_K1: JVM_IR

enum class E {
    X {
        inner class C {
            fun foo() = "OK"
        }
    }
}