// "Create class 'Foo'" "true"
// ERROR: Unresolved reference: Foo

fun test<U>(u: U) {
    val a = J(u).<caret>Foo(u)
}