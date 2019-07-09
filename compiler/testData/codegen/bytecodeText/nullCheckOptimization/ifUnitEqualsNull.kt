fun test1() {
    val a = Unit

    if (a != null) {
        println("X1")
    }

    if (a == null) {
        println("X2")
    }
}

// 0 IFNULL
// 0 IFNONNULL
// 1 X1
// 0 X2
