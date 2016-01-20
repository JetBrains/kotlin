fun test1(): Int {
    val x: String = if (true) {
        when {
            true -> <!TYPE_MISMATCH!>Any()<!>
            else -> <!NULL_FOR_NONNULL_TYPE!>null<!>
        }
    } else ""
    return x.hashCode()
}

fun test2(): Int {
    val x: String = when {
                        true -> <!TYPE_MISMATCH!>Any()<!>
                        else -> null
                    } ?: return 0
    return x.hashCode()
}