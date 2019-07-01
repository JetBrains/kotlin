// !WITH_NEW_INFERENCE
fun test1(): Int {
    val x: String = <!NI;TYPE_MISMATCH!>if (true) {
        when {
            true -> <!OI;TYPE_MISMATCH!>Any()<!>
            else -> <!OI;NULL_FOR_NONNULL_TYPE!>null<!>
        }
    } else ""<!>
    return x.hashCode()
}

fun test2(): Int {
    val x: String = <!NI;TYPE_MISMATCH!>when {
                        true -> <!OI;TYPE_MISMATCH!>Any()<!>
                        else -> null
                    } ?: return 0<!>
    return x.hashCode()
}