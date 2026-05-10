class MyList<T> {
    companion {
        operator fun <T> of(vararg x: T): MyList<T> = MyList<T>()
        operator fun <T> of(x: T): MyList<T> = MyList<T>()
    }
}

val myList: MyList<*> = <expr>["!"]</expr>

// LANGUAGE: +CollectionLiterals +CompanionBlocksAndExtensions
