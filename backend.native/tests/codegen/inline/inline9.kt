@Suppress("NOTHING_TO_INLINE")
inline fun foo(i3: Int, i4: Int): Int {
    return i3 + i3 + i4
}

fun quiz(i: Int) : Int {
    println("hello")
    return i + 1
}

fun bar(i1: Int, i2: Int): Int {
    return foo(quiz(i1), i2)
}

fun main(args: Array<String>) {
    println(bar(1, 2).toString())
}
