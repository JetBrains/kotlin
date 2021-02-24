fun blackhole(vararg a: Any?) {}

suspend fun dummy() {}

suspend fun test() {
    for (i in 0..10) {
        // Should be cleanup, since there is a back edge from the rest of the loop
        dummy()
        val a = ""
        dummy()
        blackhole(a)
    }
}

// 1 ACONST_NULL
// 2 PUTFIELD .*L\$0 : Ljava/lang/Object;