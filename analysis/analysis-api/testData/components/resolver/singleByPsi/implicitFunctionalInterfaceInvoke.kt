fun interface A {
    operator fun invoke()
}
fun foo(a: A) {
    <caret>a()
}