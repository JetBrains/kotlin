// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses

// FILE: lib.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class Test(val x: Int) {
    private companion object {
        private const val CONSTANT = "OK"
    }

    fun crash() = getInlineConstant()

    private inline fun getInlineConstant(): String {
        return CONSTANT
    }
}

// FILE: main.kt
fun box() = Test(1).crash()