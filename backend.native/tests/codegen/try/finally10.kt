fun main(args : Array<String>) {
    while (true) {
        try {
            continue
        } finally {
            println("Finally")
            break
        }
    }

    println("After")
}