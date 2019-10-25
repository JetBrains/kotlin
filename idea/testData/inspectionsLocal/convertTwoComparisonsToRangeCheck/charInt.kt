// PROBLEM: none
// DISABLE-ERRORS
fun foo(bar: Char) {
    bar > 1 && 2 > bar<caret>
}