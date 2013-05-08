// "Change parameter 'z' type of function 'foo' to '(Int) -> Unit'" "true"
fun foo(w: Int = 0, x: Int, y: Int = 0, z: (Int) -> String) {
    foo(0, 1) {<caret>}
}
