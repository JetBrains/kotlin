class MyList<T> {
    companion object {
        operator fun <T1> of(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}

fun <T2> acceptList(l: MyList<T2>) = Unit

class A

fun test() {
    acceptList<String>(<expr>["1", "2", "3"]</expr>)
}

// LANGUAGE: +CollectionLiterals
