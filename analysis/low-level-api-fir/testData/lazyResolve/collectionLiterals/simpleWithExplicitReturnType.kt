// LANGUAGE: +CollectionLiterals

fun test() {
    val a: MyCollection = [1, <caret> 2, 3]
}

interface MyCollection {
    companion object {
        operator fun of(vararg x: Int): MyCollection = object : MyCollection {}
    }
}
