// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun o<caret>f(string: String, int: Int): MyList = MyList()
    }
}
