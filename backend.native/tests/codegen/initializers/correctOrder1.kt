class Test {
    val x: Int

    init {
        x = 42
    }

    val y = x
}

fun main(args: Array<String>) {
    println(Test().y)
}