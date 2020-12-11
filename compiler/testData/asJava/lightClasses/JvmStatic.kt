// A
// WITH_RUNTIME

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