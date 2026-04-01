inline fun <T: Comparable<T>> selectMax(a: T, b: T) = if (a > b) a else b;

fun caller(a: Int, b: Int) = selectMax(a, b)

// 1 compareTo
// 0 Intrinsics.compare
// 1 IFLE
// 1 IF_ICMPLE