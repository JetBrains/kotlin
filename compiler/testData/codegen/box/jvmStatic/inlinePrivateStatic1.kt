// TARGET_BACKEND: JVM

// WITH_STDLIB

class A {
    companion object {

        fun callTest() = test()

        @JvmStatic
        private inline fun test() : String {
            return "OK"
        }
    }
}

fun box(): String {
    return A.callTest()
}