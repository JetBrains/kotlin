// A
// WITH_STDLIB

class A {
    companion object {
        @JvmStatic fun f() {

        }
    }

    object B {
        @JvmStatic
        fun g() {

        }
    }
}

// FIR_COMPARISON