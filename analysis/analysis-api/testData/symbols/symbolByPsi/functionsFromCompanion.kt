// WITH_STDLIB
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

class MyClass {
    companion object {
        fun functionFromCompanion(i: Int) = Unit

        @JvmStatic
        fun staticFunctionFromCompanion() = 4
    }
}