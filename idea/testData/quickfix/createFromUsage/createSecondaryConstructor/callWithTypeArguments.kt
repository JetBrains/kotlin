// "Create secondary constructor" "false"
// ACTION: Add parameter to constructor 'A'
// ACTION: Create function 'A'
// ACTION: Remove argument
// ERROR: No type arguments expected for constructor A()
// ERROR: Too many arguments for public constructor A() defined in A

class A

fun test() {
    val a = A<Int>(<caret>1)
}