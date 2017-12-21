package filter

fun main(args: Array<String>) {
    // Breakpoint!
    booleanArrayOf(true, false, false).asSequence().filterNot { it }.lastIndexOf(true)
}