class Foo {
    fun foo() = 1
    fun foo(i: Int) = 2
}

fun test(f: Foo) {
    f.<expr>foo(1)</expr>
}

// ISSUE: KT-74534