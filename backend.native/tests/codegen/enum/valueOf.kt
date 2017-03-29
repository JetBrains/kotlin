enum class E {
    E3,
    E1,
    E2
}

fun main(args: Array<String>) {
    println(E.valueOf("E1").toString())
    println(E.valueOf("E2").toString())
    println(E.valueOf("E3").toString())
    println(enumValueOf<E>("E1").toString())
    println(enumValueOf<E>("E2").toString())
    println(enumValueOf<E>("E3").toString())
}