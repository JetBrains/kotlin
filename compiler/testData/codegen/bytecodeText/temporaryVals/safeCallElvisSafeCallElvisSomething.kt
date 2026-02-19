fun test(a: Any?, b: Any?, c: String) =
    a?.toString() ?: b?.toString() ?: c

// 2 IFNULL
// 1 IFNONNULL
// 0 ACONST_NULL
