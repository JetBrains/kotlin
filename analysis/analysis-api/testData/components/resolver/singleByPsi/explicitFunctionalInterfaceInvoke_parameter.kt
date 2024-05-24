fun interface A {
    operator fun invoke()
}

fun foo(param: A) {
    param.invo<caret>ke()
}