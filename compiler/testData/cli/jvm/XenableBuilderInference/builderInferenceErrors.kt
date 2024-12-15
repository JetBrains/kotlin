fun main() {
    val list = buildList {
        add("one")
        add("two")
        val secondParameter = get(1)
        println(secondParameter)
    }
}
