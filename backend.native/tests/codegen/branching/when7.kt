fun main(args: Array<String>) {
    val b = args.size < 1
    val x = if (b) Any() else throw Error()
}