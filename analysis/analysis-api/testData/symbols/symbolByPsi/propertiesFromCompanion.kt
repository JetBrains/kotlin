// WITH_STDLIB
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

class MyClass {
    companion object {
        val property = 0

        const val constProperty = 1

        @JvmStatic
        val staticProperty = 2

        @JvmField
        val fieldProperty = 3

        var variable = 4

        @JvmStatic
        var staticVariable = 5

        @JvmField
        var fieldVariable = 0
    }
}