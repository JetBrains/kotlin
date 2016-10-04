fun <T : CharSequence> foo(x: Array<Any>, y: IntArray, block: (T, Int) -> Int) {
    var r: Any?

    @Suppress("UNCHECKED_CAST")
    r = block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int)

    // to prevent unused assignment diagnostic for the above statement
    <!DEBUG_INFO_SMARTCAST!>r<!>.hashCode()

    var i = 1

    if (i != 1) {
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).toInt()
    }

    val l: () -> Unit = {
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).toInt()
    }
    l()

    @Suppress("UNCHECKED_CAST")
    y[i] += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).toInt()
}
