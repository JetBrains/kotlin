fun test(a: Any?) =
    a?.toString()?.hashCode() ?: 0

// 1 ALOAD
// 0 ASTORE
// 0 ISTORE
// 0 ILOAD
// 1 POP
// 0 valueOf

// JVM_IR_TEMPLATES
// 2 DUP
// 2 IFNULL
// 0 ACONST_NULL
// 0 IFNONNULL

// JVM_TEMPLATES
// 2 DUP
// 2 IFNULL
// 0 ACONST_NULL
// 0 IFNONNULL
