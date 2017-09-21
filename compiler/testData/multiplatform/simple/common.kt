expect class Printer {
    fun print(message: String)
}

fun main(args: Array<String>) {
    val printer = Printer()
    printer.print("Hello, world!")
}
