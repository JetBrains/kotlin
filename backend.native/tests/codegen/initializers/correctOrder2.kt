class Test {
    val x: Int

    val y = 42

    init {
        x = y
    }
}

fun main(args: Array<String>) {
    println(Test().x)
}