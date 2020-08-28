fun blackhole(vararg a: Any?) {}

suspend fun dummy() {}

suspend fun test() {
    val a = null
    // a is null, known at compile time, do not spill
    dummy()
    blackhole(a)
    dummy()
}

// before and after suspension point
// 2 ACONST_NULL
// 0 PUTFIELD .*L\$0 : Ljava/lang/Object;