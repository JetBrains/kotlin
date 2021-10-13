fun test1(a: String) = a in "alpha" .. "omega"
fun test2(a: String) = a !in "alpha" .. "omega"
fun <T : Comparable<T>> test3(x: T, left: T, right: T) = x in left .. right
fun <T : Enum<T>> test4(x: T, left: T, right: T) = x in left .. right

// 0 rangeTo
// 0 contains
// 8 compareTo