fun main(args : Array<String>) {
    try {
        throw Error("Error happens")
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            println(message)
        }
    }
}