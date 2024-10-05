// API_VERSION: LATEST

suspend fun blackhole(a: Any) {}

suspend fun topLevel(a: String, b: String) {
    blackhole(a) // two spills
    blackhole(b) // two spills
}

// a and a's cleanup
// 2 PUTFIELD .*L\$0 : Ljava/lang/Object;
// 2 PUTFIELD .*L\$1 : Ljava/lang/Object;