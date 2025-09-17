// LANGUAGE: +CollectionLiterals
// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: dependency
// MODULE_KIND: Source
// FILE: dependency.kt
interface MyCollection {
    companion object {
        operator fun of(vararg x: Int) = object : MyCollection { }
    }
}

// MODULE: main(dependency)
// FILE: main.kt
fun test() {
    val a: MyCollection = [1, <caret> 2, 3]
}
