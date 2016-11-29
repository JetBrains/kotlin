fun main(args : Array<String>) {
    val x = try {
        throw Error()
        5
    } catch (e: Throwable) {
        6
    }

    println(x)
}