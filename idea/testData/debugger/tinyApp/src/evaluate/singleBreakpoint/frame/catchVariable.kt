package catchVariable

fun main(args: Array<String>) {
    try {
        throw Exception()

    }
    //Breakpoint!
    catch (e: Exception) {

    }
}

// PRINT_FRAME