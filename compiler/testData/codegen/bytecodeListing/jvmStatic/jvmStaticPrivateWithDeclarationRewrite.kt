// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// REWRITE_JVM_STATIC_IN_COMPANION

// For a private @JvmStatic function `f` in `A.Companion` with rewriting @JvmStatic declaration, we generate a private static
// method `f` in `A`, with the actual implementation.
// This exists as a counterpart to the jvmStaticPrivate.kt test, to ensure that we don't generate an accessor if we're not generating
// a proxy to the companion object.

class A {
    companion object {
        @JvmStatic
        private fun f(p: Int) {}

        @JvmStatic
        private val x = ""

        private var xx: String
            @JvmStatic get() = ""
            @JvmStatic set(value) {}
    }
}

object O {
    @JvmStatic
    private fun g(q: Int) {}

    @JvmStatic
    private val y = ""

    private var yy: String
        @JvmStatic get() = ""
        @JvmStatic set(value) {}
}
