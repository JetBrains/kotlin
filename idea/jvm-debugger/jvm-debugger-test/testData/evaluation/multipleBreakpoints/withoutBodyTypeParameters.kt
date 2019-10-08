package withoutBodyTypeParameters

import kotlin.properties.Delegates

// EXPRESSION: i
// RESULT: instance of java.lang.Integer(id=ID): Ljava/lang/Integer;
//FunctionBreakpoint!
fun <T> foo(i: T) = i

fun run(i: () -> Int) = 1

fun main(args: Array<String>) {
    foo(2)
}