// SKIP_TXT
// !LANGUAGE: +DefinitelyNonNullableTypes

inline fun <reified T : Any> foo() {}

inline fun <reified F> bar() {
    foo<F & Any>()
}
