fun main(args : Array<String>) {
    val list = arrayListOf("a", "b", "c")
    for (element in list) print(element)
    println()
    list.add("d")
    println(list.toString())

    val list2 = listOf("n", "s", "a")
    println(list2.toString())
}