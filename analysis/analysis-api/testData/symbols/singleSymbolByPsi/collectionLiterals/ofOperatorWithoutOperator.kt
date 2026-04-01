// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        fun <T1> o<caret>f(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}
