// DUMP_CFG

fun getNullableString(): String? = null
fun takeNullableString(s: String?) {}

fun test_1() {
    val x: String?

    try {
        val y = getNullableString()!! // 3
        x = getNullableString()
    } finally {
        Unit
    }

    takeNullableString(x)
}

fun test_2() {
    val x: String?

    try {
        val y = getNullableString()
        x = getNullableString()
    } finally {
        Unit
    }

    takeNullableString(x)
}
