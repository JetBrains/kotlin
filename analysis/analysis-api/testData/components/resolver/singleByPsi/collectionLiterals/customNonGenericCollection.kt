class MyList {
    companion object {
        operator fun of(vararg vals: String): MyList = MyList()
    }
}

fun acceptList(l: MyList) = Unit

fun test() {
    acceptList(<expr>["1", "2", "3"]</expr>)
}

// LANGUAGE: +CollectionLiterals
