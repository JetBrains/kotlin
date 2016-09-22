class A
fun foo(a: Any) {
    // Do not suggest 'A' as an expression to parenthesize
    a as A.par<caret>
}
