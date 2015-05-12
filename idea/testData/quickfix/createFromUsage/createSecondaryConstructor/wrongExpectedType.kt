// "Create secondary constructor" "false"
// ACTION: Add parameter to constructor 'A'
// ACTION: Change 'b' type to 'A'
// ACTION: Create function 'A'
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>B</td></tr><tr><td>Found:</td><td>A</td></tr></table></html>
// ERROR: Too many arguments for public constructor A() defined in A

class A

class B

fun test() {
    val b: B = A(<caret>1)
}