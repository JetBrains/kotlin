// !WITH_NEW_INFERENCE
class MyClass(var p: String?)

fun bar(s: String): Int {
    return s.length
}

fun foo(m: MyClass): Int {
    m.p = "xyz"
    return <!INAPPLICABLE_CANDIDATE!>bar<!>(m.p)
}
