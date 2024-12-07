fun interface A {
    operator fun invoke()
}

val globalA: A = A {}

fun foo() {
    globalA.invo<caret>ke()
}