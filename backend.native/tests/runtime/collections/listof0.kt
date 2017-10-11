package runtime.collections.listof0

import kotlin.test.*

@Test fun runTest() {
    main(arrayOf("a"))
}

fun main(args : Array<String>) {
    val nonConstStr = args[0]
    val list = arrayListOf(nonConstStr, "b", "c")
    for (element in list) print(element)
    println()
    list.add("d")
    println(list.toString())

    val list2 = listOf("n", "s", nonConstStr)
    println(list2.toString())
}