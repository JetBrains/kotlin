// SKIP_TXT
// !LANGUAGE: +DefinitelyNonNullableTypes

inline fun <reified T : Any> foo() {}

inline fun <reified F> bar() {
    foo<<!DEFINITELY_NON_NULLABLE_AS_REIFIED!>F & Any<!>>()
}
