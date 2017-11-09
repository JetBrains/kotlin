// "Create actual class for platform JVM" "true"

expect class <caret>WithNested {
    fun foo(): Int

    class Nested {
        fun bar()
    }

    inner class Inner {
        fun baz()
    }
}
