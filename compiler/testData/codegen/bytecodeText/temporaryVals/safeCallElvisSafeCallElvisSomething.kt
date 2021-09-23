fun test(a: Any?, b: Any?, c: String) =
    a?.toString() ?: b?.toString() ?: c

// 2 IFNULL
// 1 ACONST_NULL

// JVM_IR_TEMPLATES
// 1 IFNONNULL

// JVM_TEMPLATES
// 2 IFNONNULL