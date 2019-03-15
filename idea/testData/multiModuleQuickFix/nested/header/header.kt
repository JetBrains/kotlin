// "Create actual class for module testModule_JVM (JVM)" "true"

expect class <caret>WithNested {
    fun foo(): Int

    class Nested {
        fun bar()
    }

    inner class Inner {
        fun baz()
    }
}
