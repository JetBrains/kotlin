// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtFunction
// OPTIONS: usages
// FIND_BY_REF

fun foo(p: Pair<Int, Int>) {
    p.<caret>component1()
    val (x, y) = p
}

fun foo() {
    val (x, y) = 1 to "a"
}