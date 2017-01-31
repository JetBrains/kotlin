// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages
fun foo(<caret>param: String) {

}

fun bar() {
    foo(param = "")
}