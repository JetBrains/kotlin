// CHECK_LOCAL_SYMBOL_ID_RESTORATION

class A {
    fun x() {
        val <expr>a</expr> = doSmth("str")
    }
}

fun doSmth(i: String) = 4
