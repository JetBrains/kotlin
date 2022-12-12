// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// REWRITE_JVM_STATIC_IN_COMPANION

class A {

    companion object {

        private val a = "test1"
        @JvmStatic
        private val b = "test2"
        private const val c = "test3"

        @JvmStatic
        fun f1(): String = a

        @JvmStatic
        fun f2(): String = b

        @JvmStatic
        fun f3(): String = c
    }
}

fun box(): String {
    if (A.f1() != "test1") return "fail"
    if (A.f2() != "test2") return "fail2"
    if (A.f3() != "test3") return "fail3"
    return "OK"
}
