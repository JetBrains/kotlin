val a : Int? = 10

fun foo() = a?.toString()

// JVM_TEMPLATES
// 1 IFNULL

// JVM_IR_TEMPLATES
// 1 IFNONNULL
