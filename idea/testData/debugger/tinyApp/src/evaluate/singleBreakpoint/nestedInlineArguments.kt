package nestedInlineArguments

fun main(args: Array<String>) {
    val list1 = listOf("a")
    val list2 = listOf(Bar())
    list1.map { list2.foo(1) }
}

inline fun <reified T: Bar> List<T>.foo(key: Int): T? {
    // EXPRESSION: it.i
    // RESULT: 1: I
    //Breakpoint! (lambdaOrdinal = 1)
    return this.firstOrNull { it.i == key }
}

open class Foo
open class Bar {
    val i = 1
}