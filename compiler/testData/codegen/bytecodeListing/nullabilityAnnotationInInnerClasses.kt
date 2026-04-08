// See KT-62714

enum class E {
    X {
        inner class C {
            fun foo() = "OK"
        }
    }
}