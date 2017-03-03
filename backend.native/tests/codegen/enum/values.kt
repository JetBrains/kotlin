enum class E {
    E3,
    E1,
    E2
}

fun main(args: Array<String>) {
    println(E.values()[0].toString())
    println(E.values()[1].toString())
    println(E.values()[2].toString())
}