// "Change parameter 'x' type of function 'bar.foo' to '(HashSet<Int>) -> Int'" "true"
package bar
fun foo(w: Int = 0, x: Int, y: Int = 0, z: (Int) -> Int = {42}) {
    foo(1, { a: java.util.HashSet<Int> -> 42}<caret>, 1)
}
