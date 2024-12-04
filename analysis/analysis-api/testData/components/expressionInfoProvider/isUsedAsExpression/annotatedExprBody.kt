fun main(args: Array<String>) {
    val x = args + (@OptIn(Deprecated::class) <expr>args</expr>)
}