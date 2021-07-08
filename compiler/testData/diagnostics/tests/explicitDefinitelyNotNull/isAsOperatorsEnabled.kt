// SKIP_TXT
// !LANGUAGE: +DefinitelyNotNullTypeParameters

fun Any.bar() {}
fun Boolean.baz() {}

var x: Int = 0

inline fun <reified T> foo(v: Any?): T {
    <!DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL!>if (x > 0) 1 else v as T!!<!>
    <!DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL!>if (x > 1) 2 else v as? T!!<!>
    <!DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL!>if (x > 2) 3 else v is T<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>
    <!DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL!>if (x > 3) 4 else v !is T<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>

    <!DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL!>return v as T<!UNNECESSARY_NOT_NULL_ASSERTION, UNREACHABLE_CODE!>!!<!><!>
}
