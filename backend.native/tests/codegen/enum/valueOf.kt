enum class E {
    E3,
    E1,
    E2
}

fun main(args: Array<String>) {
    println(E.valueOf("E1").toString())
    println(E.valueOf("E2").toString())
    println(E.valueOf("E3").toString())
}