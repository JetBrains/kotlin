fun main(args : Array<String>) {
    val x = try {
        5
    } catch (e: Throwable) {
        6
    }

    println(x)
}