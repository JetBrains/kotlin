class Test {
    companion object {
        init {
            println("Init Test")
        }
    }
}

fun main(args : Array<String>) {
    val t1 = Test()
    val t2 = Test()
    println("Done")
}