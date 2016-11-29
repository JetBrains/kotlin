fun main(args : Array<String>) {
    do {
        try {
            break
        } finally {
            println("Finally 1")
        }
    } while (false)

    var stop = false
    while (!stop) {
        try {
            stop = true
            continue
        } finally {
            println("Finally 2")
        }
    }

    println("After")
}