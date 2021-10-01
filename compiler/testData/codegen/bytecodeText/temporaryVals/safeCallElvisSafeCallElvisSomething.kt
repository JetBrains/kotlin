fun test(a: Any?, b: Any?, c: String) =
    a?.toString() ?: b?.toString() ?: c

// JVM_IR_TEMPLATES
// 2 IFNULL
// 1 IFNONNULL
// 0 ACONST_NULL

// JVM_TEMPLATES
// 2 IFNULL
// 1 ACONST_NULL
// 2 IFNONNULL