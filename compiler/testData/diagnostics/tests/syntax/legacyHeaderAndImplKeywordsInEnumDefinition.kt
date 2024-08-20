// FIR_IDENTICAL
// ISSUE: KT-52315

enum class Foo(val id: Int) {
    header(1)
}

enum class Bar(val id: Int) {
    impl(2)
}

fun testHeader(): Int = Foo.header.id

fun testImpl(): Int = Bar.impl.id