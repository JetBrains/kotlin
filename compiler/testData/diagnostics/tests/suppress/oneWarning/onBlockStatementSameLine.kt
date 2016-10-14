fun <T : CharSequence> foo(x: Array<Any>, block: (T, Int) -> Int) {
    var r: Any?

    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Suppress("UNCHECKED_CAST") r<!> = block(<!UNCHECKED_CAST!>x[0] as T<!>, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int)

    // to prevent unused assignment diagnostic for the above statement
    <!DEBUG_INFO_SMARTCAST!>r<!>.hashCode()

    var i = 1

    if (i != 1) {
        <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Suppress("UNCHECKED_CAST") i<!> += block(<!UNCHECKED_CAST!>x[0] as T<!>, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).toInt()
    }

    if (i != 1) @Suppress("UNCHECKED_CAST")
        i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).toInt()

    if (i != 1) <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Suppress("UNCHECKED_CAST") i<!> += block(<!UNCHECKED_CAST!>x[0] as T<!>, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).toInt()
}
