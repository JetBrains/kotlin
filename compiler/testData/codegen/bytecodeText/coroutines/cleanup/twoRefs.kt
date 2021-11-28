suspend fun blackhole(a: Any?) {}

suspend fun cleanUpExample(a: String, b: String) {
    blackhole(a) // 1
    blackhole(b) // 2
}

// 3 ACONST_NULL
// 2 PUTFIELD .*L\$0 : .*;
