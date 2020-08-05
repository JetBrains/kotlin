fun testChar(x: Char, a: Char, b: Char) =
        if (x !in a .. b) "no" else "yes"

fun testInt(x: Int, a: Int, b: Int) =
        if (x !in a .. b) "no" else "yes"

fun testLong(x: Long, a: Long, b: Long) =
        if (x !in a .. b) "no" else "yes"

fun testDouble(x: Double, a: Double, b: Double) =
        if (x !in a .. b) "no" else "yes"

fun testString(x: String, a: String, b: String) =
        if (x !in a .. b) "no" else "yes"

fun testCollection(x: Any, xs: List<Any>) =
        if (x !in xs) "no" else "yes"

// 0 IXOR
