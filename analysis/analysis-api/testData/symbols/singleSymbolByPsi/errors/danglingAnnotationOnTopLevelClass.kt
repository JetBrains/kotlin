// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
annotation class Ann

@Ann(value = {
    @Ann
    class <caret>LocalClass
}