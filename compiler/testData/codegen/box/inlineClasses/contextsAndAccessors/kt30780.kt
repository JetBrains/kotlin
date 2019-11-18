// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Test(val x: Int) {
    private companion object {
        private const val CONSTANT = "OK"
    }

    fun crash() = getInlineConstant()

    private inline fun getInlineConstant(): String {
        return CONSTANT
    }
}

fun box() = Test(1).crash()