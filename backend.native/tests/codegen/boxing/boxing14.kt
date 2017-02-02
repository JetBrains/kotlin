fun main(args: Array<String>) {
    42.println()
}

fun <T> T.println() = println(this.toString())