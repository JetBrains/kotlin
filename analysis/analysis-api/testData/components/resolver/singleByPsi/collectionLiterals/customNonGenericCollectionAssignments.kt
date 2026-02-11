class MyList {
    companion object {
        operator fun of(vararg vals: String): MyList = MyList()
    }
}

val globalList: MyList = <expr>[]</expr>

// LANGUAGE: +CollectionLiterals
