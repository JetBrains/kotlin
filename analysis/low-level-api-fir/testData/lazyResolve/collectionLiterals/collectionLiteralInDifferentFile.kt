// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// LANGUAGE: +CollectionLiterals

// FILE: collectionLiteralType.kt
interface MyCollection {
    companion object {
        operator fun of(vararg x: Int) = object : MyCollection { }
    }
}

// FILE: main.kt
fun test() {
    val a: MyCollection = [1, <caret> 2, 3]
}
