// LANGUAGE: +CollectionLiterals
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

class MyList {
    companion object {
        operator fun o<caret>f(string: String, int: Int): MyList = MyList()
    }
}
