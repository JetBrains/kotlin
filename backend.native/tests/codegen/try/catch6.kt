fun main(args : Array<String>) {

    try {
        println("Before")
        foo()
        println("After")
    } catch (e: Exception) {
        println("Caught Exception")
    } catch (e: Error) {
        println("Caught Error")
    }

    println("Done")
}

fun foo() {
    try {
        throw Error("Error happens")
    } catch (e: Exception) {
        println("Caught Exception")
    }
}