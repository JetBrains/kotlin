// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
annotation class Ann

class C {
    @Ann(value = {
        @Ann
        val <caret>localVal = 0
    }
}