// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
// FIR_IGNORE

fun test() {
    do {
        val <caret>message = "test"
        println(message)
    } while (message.isEmpty())
}