fun foo(x: String): String? = x

fun calc(x: String?): Int {
    // Smart cast because of x!! in receiver
    foo(x!!)?.subSequence(0, x.length)?.length
    // Smart cast because of x!! in receiver
    return x.length
}
