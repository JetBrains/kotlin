fun test(a: Any?) = a!! as String

// 1 checkNotNull
// JVM_IR_TEMPLATES
// 0 IFNULL
// 0 IFNONNULL
// 0 NullPointerException
// 0 ASTORE
