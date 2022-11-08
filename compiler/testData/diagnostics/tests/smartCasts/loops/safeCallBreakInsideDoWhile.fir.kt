fun foo(x: String): String? = x

fun calc(x: String?, y: String?): Int {
    do {
        // Smart cast because of x!! in receiver
        foo(x!!)?.subSequence(0, if (x.length > 0) 5 else break)
        y!!.length
        // x is not null in condition but we do not see it yet
    } while (x.length > 0)
    // y is nullable because of break
    y.length
    // x is not null, at least in theory
    return x.length
}
