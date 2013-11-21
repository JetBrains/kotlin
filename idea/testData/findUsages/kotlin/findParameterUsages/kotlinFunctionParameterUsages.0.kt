// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetParameter
// OPTIONS: usages
fun foo<T>(<caret>t: T): T {
    println(t)
    return t
}