// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

enum class MyEnumClass {
    FirstEntry {
        fun a() {}
    },
    SecondEntry,
    ThirdEntry {
        fun one() {}
        fun one(i: Int) {}
    }
}
