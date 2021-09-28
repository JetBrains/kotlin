fun test(xs: IntArray, dx: Int) {
    for (i in xs.indices) {
        xs[i] += dx
    }
}

// JVM_IR_TEMPLATES
// 5 ALOAD
// 6 ILOAD

