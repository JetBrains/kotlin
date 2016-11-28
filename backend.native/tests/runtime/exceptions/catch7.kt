fun main(args : Array<String>) {
    try {
        foo()
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            println(message)
        }
    }
}

fun foo() {
    throw Error("Error happens")
}