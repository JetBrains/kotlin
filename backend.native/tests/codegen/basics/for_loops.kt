fun main(args: Array<String>) {

    // Simple loops
    for (i in 0..4) {
        print(i)
    }
    println()

    for (i in 0 until 4) {
        print(i)
    }
    println()

    for (i in 4 downTo 0) {
        print(i)
    }
    println()
    println()

    // Steps
    for (i in 0..4 step 2) {
        print(i)
    }
    println()

    for (i in 0 until 4 step 2) {
        print(i)
    }
    println()

    for (i in 4 downTo 0 step 2) {
        print(i)
    }
    println()
    println()


    // Two steps
    for (i in 0..6 step 2 step 3) {
        print(i)
    }
    println()

    for (i in 0 until 6 step 2 step 3) {
        print(i)
    }
    println()

    for (i in 6 downTo 0 step 2 step 3) {
        print(i)
    }
    println()
    println()
}