class Class {
    default object {
        val p = 239
    }
}

fun f() {
    println(Class.<caret>p)
}