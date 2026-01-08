// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        fun <T1> o<caret>f(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}
