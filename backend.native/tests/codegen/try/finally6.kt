fun main(args : Array<String>) {
    println(foo())
}

fun foo(): Int {
    try {
        println("Done")
        return 0
    } finally {
        println("Finally")
        return 1
    }

    println("After")
    return 2
}