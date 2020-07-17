class Declaration {
    operator fun <T> <caret>set(i: Int, e: T) {
        println(i)
        println(e)
    }
}

fun call() {
    val declaration = Declaration()
    declaration.set(4, "")
    declaration[42] = declaration
    Declaration()[{ 42 }()] = { 4 }
}