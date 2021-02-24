class Cell<T>(val x: T)

typealias StringCell = Cell<String>

fun box(): String =
        StringCell("O").x + Cell("K").x