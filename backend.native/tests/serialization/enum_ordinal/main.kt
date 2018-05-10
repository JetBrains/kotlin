fun main(args: Array<String>) {
    println(Color.RED.ordinal)
    println(Color.GREEN.ordinal)
    println(Color.BLUE.ordinal)
    val color = when (determineColor(args.size)) {
        Color.RED -> println("r")
        Color.GREEN -> println("g")
        Color.BLUE -> println("b")
        Color.CYAN -> println("c")
        Color.MAGENTA -> println("m")
        Color.YELLOW -> println("y")

    }
}