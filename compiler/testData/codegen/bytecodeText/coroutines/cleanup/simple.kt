fun blackhole(vararg a: Any?) {}

suspend fun dummy() {}

suspend fun test() {
    val a = ""
    dummy()
    blackhole(a)
    // a is dead, cleanup
    dummy()
}

// 1 ACONST_NULL
// 2 PUTFIELD .*L\$0 : Ljava/lang/Object;