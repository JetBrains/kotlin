// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND: JVM_IR
// FILE: Foo.kt

private const val OUTER_PRIVATE = 20

class Foo {
    companion object {
        private const val LOCAL_PRIVATE = 20
    }

    fun foo() {
        // Access to the property use getstatic on the backed field
        LOCAL_PRIVATE
        // Access to the property requires an invokestatic
        OUTER_PRIVATE
    }
}

// 1 INVOKESTATIC
// 1 PUTSTATIC
// 2 GETSTATIC
