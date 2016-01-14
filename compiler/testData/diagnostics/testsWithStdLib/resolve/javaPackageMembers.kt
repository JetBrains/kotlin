fun fn(): Nothing = throw java.lang.RuntimeException("oops")

val x: Nothing = throw java.lang.RuntimeException("oops")

class SomeClass {
    fun method() {
        throw java.lang.AssertionError("!!!")
    }
}