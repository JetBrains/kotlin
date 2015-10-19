// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtTypeParameter
// OPTIONS: usages
fun <<caret>T> T.foo(t: T, list: List<T>): T {
    return t
}
