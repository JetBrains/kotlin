// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

// KT-8596 Rewrite at slice LEXICAL_SCOPE for nested class constructor reference in an argument position

class K {
    class Nested
}

fun foo(f: Any) {}

fun test1() {
    foo(K::Nested)
}

// KT-10567 Error: Rewrite at slice LEXICAL_SCOPE key: REFERENCE_EXPRESSION

class Foo(val a: String, val b: String)

fun test2() {
    val prop : Foo.() -> String = if (true) {
        Foo::a
    } else {
        Foo::b
    }
}
