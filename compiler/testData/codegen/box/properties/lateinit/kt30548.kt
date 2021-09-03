
// IGNORE_BACKEND: JVM

class Test {
    companion object {
        private lateinit var INSTANCE: String
        fun foo() {
            INSTANCE
        }
    }
}

fun box(): String {
    try {
        Test.foo()
        return "'Test.foo()' should throw"
    } catch (e: Exception) {
        return "OK"
    }
}