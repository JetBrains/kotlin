// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
annotation class Ann

class C {
    @Ann(value = {
        @Ann
        class <caret>LocalClass
    }
}