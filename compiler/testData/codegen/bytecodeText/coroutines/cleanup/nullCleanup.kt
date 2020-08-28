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

// 2 PUTFIELD .*L\$0 : Ljava/lang/Object;

// JVM_TEMPLATES:
// just before suspension point
// 1 ACONST_NULL

// JVM_IR_TEMPLATES:
// two stores to initialize the `a` variable and one null constant to store in the spill slot.
// 3 ACONST_NULL
