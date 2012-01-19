package bbb

fun main(args: Array<String>) {
    val h = aaa.hello()
    if (h != 17) {
        throw Exception()
    }
    System.out?.println("It is 17!")
}
