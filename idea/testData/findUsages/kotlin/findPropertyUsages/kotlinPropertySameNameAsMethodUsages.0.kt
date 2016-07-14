// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
class P

interface C {
    val <caret>p: P  // set caret at "p" and try find usages
    fun p(param: Any)
}

fun C.usageCorrect() {
    println(p)
}

fun C.usageIncorrect() {
    p("ok")
}