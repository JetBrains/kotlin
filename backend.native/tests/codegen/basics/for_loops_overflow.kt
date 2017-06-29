fun main(args: Array<String>) {
    for (i in Int.MAX_VALUE - 1 .. Int.MAX_VALUE) { print(i); print(' ') }; println()
    for (i in Int.MAX_VALUE - 1 until Int.MAX_VALUE) { print(i); print(' ') }; println()
    for (i in Int.MIN_VALUE + 1 downTo Int.MIN_VALUE) { print(i); print(' ') }; println()

    val M = Int.MAX_VALUE / 2
    for (i in M + 4..M + 10 step M)  { print(i); print(' ') }; println()
}