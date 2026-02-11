open class MyList {
    companion object {
        operator fun of(vararg x: Int): MyList = MyList()
    }
}

class MyChildList : MyList() {
    companion object {
        operator fun of(vararg x: Int): MyChildList = MyChildList()
    }
}

fun test() {
    val x: MyList = when {
        true -> MyChildList()
        else -> <expr>[1, 2, 3]</expr>
    }
}

// LANGUAGE: +CollectionLiterals
// COMPILATION_ERRORS