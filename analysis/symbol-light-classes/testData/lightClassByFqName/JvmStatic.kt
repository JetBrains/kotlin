// A
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

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
