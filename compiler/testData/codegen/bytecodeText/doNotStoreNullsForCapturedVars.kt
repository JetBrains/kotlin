fun runNoInline(block: ()-> Unit): Unit {
    block()
}

fun use(x: Int) {}

fun test(): Unit {
    var x = 0
    runNoInline {
        use(x)
    }
}

// 0 ACONST_NULL