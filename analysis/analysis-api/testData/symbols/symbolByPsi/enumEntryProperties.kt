// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

enum class MyEnumClass {
    FirstEntry {
        val a: Int = 1
    },
    SecondEntry,
    ThirdEntry {
        val b = 2
        val Int.d get() = 2
    }
}
