class Class {
    val p = 239

    fun f() {
        println(p)
    }
}

fun f() {
    println(Class().<caret>p)
}