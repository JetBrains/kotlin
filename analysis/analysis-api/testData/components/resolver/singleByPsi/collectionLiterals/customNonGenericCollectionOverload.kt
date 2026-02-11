class MyList {
    companion object {
        operator fun of(vararg vals: String): MyList = MyList()
        operator fun of(string1: String, another: String): MyList = MyList()
    }
}

val globalList: MyList = <expr>["1", "2"]</expr>

// LANGUAGE: +CollectionLiterals
