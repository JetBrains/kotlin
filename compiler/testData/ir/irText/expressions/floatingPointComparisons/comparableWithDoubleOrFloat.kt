fun testD(x: Comparable<Double>, y: Comparable<Double>) = x is Double && y is Double && x < y

fun testF(x: Comparable<Float>, y: Comparable<Float>) = x is Float && y is Float && x < y