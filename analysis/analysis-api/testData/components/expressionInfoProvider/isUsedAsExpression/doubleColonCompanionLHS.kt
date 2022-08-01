class C {
    companion object {
        fun t() = x
        val x = 45
    }
}

fun test(): Int {
    return (<expr>C.Companion</expr>::x).get()
}