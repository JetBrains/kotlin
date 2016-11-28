fun main(args : Array<String>) {

    try {
        println("Try")
        throw Error("Error happens")
        println("After throw")
    } catch (e: Error) {
        println("Caught Error")
    } finally {
        println("Finally")
    }

    println("Done")
}