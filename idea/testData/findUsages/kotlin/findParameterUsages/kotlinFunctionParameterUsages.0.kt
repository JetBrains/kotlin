// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages
fun <T> foo(<caret>t: T): T {
    println(t)
    return t
}

fun bar(t: String) {
    print(t)
}

fun usage() {
    foo(t = ":)")
}

fun falseUsage() {
    bar(t = "")
}