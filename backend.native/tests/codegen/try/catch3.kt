fun main(args : Array<String>) {
    try {
        println("Before")
        throw Error("Error happens")
        println("After")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")
}