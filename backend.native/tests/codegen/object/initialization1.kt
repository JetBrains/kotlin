class Test {
    constructor() {
        println("constructor1")
    }

    constructor(x: Int) : this() {
        println("constructor2")
    }

    init {
        println("init")
    }

    val f = println("field")
}

fun main(args: Array<String>) {
    Test()
    Test(1)
}