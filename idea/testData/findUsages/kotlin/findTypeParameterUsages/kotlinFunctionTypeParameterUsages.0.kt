// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetTypeParameter
// OPTIONS: usages
fun <<caret>T> T.foo(t: T, list: List<T>): T {
    return t
}