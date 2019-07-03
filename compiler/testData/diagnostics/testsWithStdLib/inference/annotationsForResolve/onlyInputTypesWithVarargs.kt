fun test1(ns: String?, vararg nullableStrs: String?) {
    nullableStrs.contains("x")
    nullableStrs.contains(ns)
    "x" in nullableStrs
    ns in nullableStrs
}

fun test2(ns: String?, vararg nonNullableStrs: String) {
    nonNullableStrs.contains("x")
    nonNullableStrs.contains(ns)
    "x" in nonNullableStrs
    ns in nonNullableStrs
}

fun test3(ns: String?, nullableStrs: Array<out String?>, nonNullableStrs: Array<out String>) {
    nullableStrs.contains("x")
    nonNullableStrs.contains("x")

    nullableStrs.contains(ns)
    nonNullableStrs.contains(ns)

    "x" in nullableStrs
    "x" in nonNullableStrs
    ns in nullableStrs
    ns in nonNullableStrs
}
