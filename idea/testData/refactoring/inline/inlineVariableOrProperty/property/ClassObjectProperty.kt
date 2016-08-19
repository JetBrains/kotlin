class Class {
    companion object {
        val p = 239
    }
}

fun f() {
    println(Class.<caret>p)
}