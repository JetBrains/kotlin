// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetParameter
// OPTIONS: usages
fun foo<T>(<caret>t: T): T {
    println(t)
    return t
}

fun usage() {
    foo(t = ":)")
}