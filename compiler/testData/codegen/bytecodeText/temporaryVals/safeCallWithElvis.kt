fun test(a: Any?) =
    a?.toString()?.hashCode() ?: 0

// 1 ALOAD
// 0 ASTORE
// 0 ISTORE
// 0 ILOAD
// 1 POP
// 0 valueOf

// JVM_IR_TEMPLATES
// 1 DUP
// 1 IFNULL

// JVM_TEMPLATES
// 2 DUP
// 2 IFNULL
