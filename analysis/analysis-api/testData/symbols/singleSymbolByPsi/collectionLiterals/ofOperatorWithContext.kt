// LANGUAGE: +CollectionLiterals +ContextParameters

class MyList<T> {
    companion object {
        context(_: Int)
        operator fun <T1> o<caret>f(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}
