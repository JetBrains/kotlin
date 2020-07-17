class Declaration {
    operator fun <caret>get(p: Int) = p
}

fun call() {
    val declaration = Declaration()
    val vg1 = declaration.get(4)
    val vg2 = declaration[42].let { it + 1 }
    val vg3 = Declaration()[42].let { it + 1 }
}