// WITH_STDLIB

// For a private @JvmStatic function `f` in `A.Companion`, we generate:
// 1) private instance method `f` in `A.Companion`, with the actual implementation;
// 2) public static method `access$f` in `A.Companion` which calls `A.Companion.f`;
// 3) private static method `f` in `A` which calls `A.Companion.access$f`.
// (which is basically the same as all @JvmStatic companion functions, except that we also need an accessor here.)
//
// This might seem like an overkill, but there are actually some use cases, namely private static helpers for JNI (see KT-46181).

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
