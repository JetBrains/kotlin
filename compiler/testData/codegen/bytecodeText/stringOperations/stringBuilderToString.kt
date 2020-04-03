val chars = listOf('O', 'K')

fun box(): String {
    val b = StringBuilder()
    for (c in chars) {
        b.append(c)
    }
    return b.toString()
}

// 0 INVOKESTATIC java/lang/String.valueOf
// 1 INVOKEVIRTUAL java/lang/StringBuilder.toString