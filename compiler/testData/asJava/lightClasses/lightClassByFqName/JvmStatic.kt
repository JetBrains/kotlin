// A
// WITH_STDLIB

class A {
    companion object {
        @JvmStatic fun f() { }
    }

    object B {
        @JvmStatic fun g() { }
    }

    interface I {
        companion object {
            @JvmStatic fun h() { }
        }

        object C {
            @JvmStatic
            fun i() { }
        }
    }
}

// FIR_COMPARISON