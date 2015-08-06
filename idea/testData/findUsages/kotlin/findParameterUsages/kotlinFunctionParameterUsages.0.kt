// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetParameter
// OPTIONS: usages
fun foo<T>(<caret>t: T): T {
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