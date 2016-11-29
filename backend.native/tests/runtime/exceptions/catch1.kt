fun main(args : Array<String>) {
    try {
        println("Before")
        foo()
        println("After")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")
}

fun foo() {
    throw Error("Error happens")
    println("After in foo()")
}