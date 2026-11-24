// LANGUAGE: +CompanionBlocks +CompanionExtensions

class A {
    companion {
        val x: Int
            get() {
                val <expr>a</expr> = doSmth("str")
                return a
            }
    }
}

fun doSmth(i: String) = 4
