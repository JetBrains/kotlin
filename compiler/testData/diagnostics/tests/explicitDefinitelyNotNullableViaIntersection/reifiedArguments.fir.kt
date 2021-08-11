// SKIP_TXT
// !LANGUAGE: +DefinitelyNotNullTypeParameters

inline fun <reified T : Any> foo() {}

inline fun <reified F> bar() {
    foo<F & Any>()
}
