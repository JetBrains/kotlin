fun main(args : Array<String>) {
    val x = try {
        throw Error()
    } catch (e: Throwable) {
        6
    }

    println(x)
}