class MyList<T> {
    companion object {
        operator fun <T> of(vararg vals: T): MyList<T> = MyList<T>()
    }
}

fun test() {
    val lst1: MyList<String> = <expr>[]</expr>
}

// LANGUAGE: +CollectionLiterals
