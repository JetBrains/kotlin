fun main(args : Array<String>) {
    try {
        try {
            println("Before")
            foo()
            println("After")
        } catch (e: Exception) {
            println("Caught Exception")
        }

        println("After nested try")

    } catch (e: Error) {
        println("Caught Error")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")
}

fun foo() {
    throw Error("Error happens")
    println("After in foo()")
}