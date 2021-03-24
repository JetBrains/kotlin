package constructor

// EXPRESSION: a
// RESULT: 1: I
//FunctionBreakpoint!
class C(val a: Int)

fun main(args: Array<String>) {
    C(1)
}
