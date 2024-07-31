fun <T : CharSequence> foo(x: Array<Any>, y: IntArray, block: (T, Int) -> Int) {
    var r: Any?

    @Suppress("UNCHECKED_CAST")
    // comment
    /* comment */
    r = block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int)

    // to prevent unused assignment diagnostic for the above statement
    r.hashCode()

    var i = 1

    if (i != 1) {
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>
    }

    if (i != 1)
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>

    if (i != 2)
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>
    else
        @Suppress("UNCHECKED_CAST")
        i += block(x[1] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>

    while (i != 1)
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>

    do
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>
    while (i != 1)

    for (j in 1..100)
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>

    when (i) {
        1 ->
            @Suppress("UNCHECKED_CAST")
            i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>
    }

    val l: () -> Unit = {
        @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>
    }
    l()

    // many empty new lines
    @Suppress("UNCHECKED_CAST")


    y[i] += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>
}
