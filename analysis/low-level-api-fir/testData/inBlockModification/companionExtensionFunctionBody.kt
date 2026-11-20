// LANGUAGE: +CompanionBlocks +CompanionExtensions

class A

companion fun A.x(): Int {
    val <expr>a</expr> = doSmth("str")
    return a
}

fun doSmth(i: String) = 4
