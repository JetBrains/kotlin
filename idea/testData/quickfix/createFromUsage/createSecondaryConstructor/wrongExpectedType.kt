// "Create secondary constructor" "false"
// ACTION: Add parameter to constructor 'A'
// ACTION: Change type of 'b' to 'A'
// ACTION: Create function 'A'
// ERROR: Type mismatch: inferred type is A but B was expected
// ERROR: Too many arguments for public constructor A() defined in A

class A

class B

fun test() {
    val b: B = A(<caret>1)
}