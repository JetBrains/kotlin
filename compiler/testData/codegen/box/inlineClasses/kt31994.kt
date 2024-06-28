// IGNORE_BACKEND: JVM

fun box(): String =
    testBug(null)

fun testBug(test: Test?): String =
    test?.Inner()?.thing ?: "OK"

class Test(val name: String) {
    inner class Inner {
        val thing: String
            get() = name
    }
}
