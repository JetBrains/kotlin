fun main(args : Array<String>) {

    try {
        try {
            println("Try")
            throw Error("Error happens")
            println("After throw")
        } finally {
            println("Finally")
        }

        println("After nested try")

    } catch (e: Error) {
        println("Caught Error")
    }

    println("Done")
}