fun main(args : Array<String>) {
    println(foo())
}

fun foo(): Int {
    try {
        try {
            return 42
        } finally {
            println("Finally 1")
        }
    } finally {
        println("Finally 2")
    }

    println("After")
    return 2
}