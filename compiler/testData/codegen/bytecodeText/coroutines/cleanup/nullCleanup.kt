fun blackhole(vararg a: Any?) {}

suspend fun dummy() {}

suspend fun test() {
    var a: String? = ""
    dummy()
    blackhole(a)
    a = null
    // a is null, known at compile time, do not spill, but cleanup
    dummy()
    blackhole(a)
}

// jsut before suspension point
// 1 ACONST_NULL
// 2 PUTFIELD .*L\$0 : Ljava/lang/Object;