class A {}

fun A.foo() {}

fun bar(a: A) {
    a.<caret>foo()
}
