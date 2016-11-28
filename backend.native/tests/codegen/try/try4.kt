fun main(args : Array<String>) {
    val x = try {
        println("Try")
        5
    } catch (e: Throwable) {
        throw e
    }

    println(x)
}