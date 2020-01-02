fun foo(x: String): String? = x

fun calc(x: String?): Int {
    do {
        // Smart cast because of x!! in receiver
        foo(x!!)?.subSequence(0, x.length)
        // Smart cast because of x!! in receiver
        if (x.length == 0) break
    } while (true)
    // Here x is also not null
    return x.length
}
