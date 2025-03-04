// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

class A {
    var some<caret>hing: Int
        inline get() = 0
        inline set(value) {}
}
