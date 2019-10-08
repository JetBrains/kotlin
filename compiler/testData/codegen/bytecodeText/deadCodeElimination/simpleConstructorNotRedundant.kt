class A
fun box() {
    val x: A? = A()
    val y: A?
    if (1 == 0) {
        y = x
    }
    else {
        y = null
    }

    y!!
}

// 0 IFNULL
// 0 ATHROW
// 0 checkNotNull
// 1 throwNpe

// JVM_TEMPLATES:
// 1 IFNONNULL

// JVM_IR_TEMPLATES:
// 0 IFNONNULL
