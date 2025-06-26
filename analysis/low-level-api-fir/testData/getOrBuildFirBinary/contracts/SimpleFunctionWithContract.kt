// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
fun foo(s: String?) contract [returns() implies (s != null)] {
    s ?: throw IllegalArgumentException()
}
