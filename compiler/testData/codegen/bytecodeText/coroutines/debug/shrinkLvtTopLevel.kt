suspend fun blackhole(a: Any) {}

suspend fun topLevel(a: String, b: String) {
    blackhole(a) // one spill
    blackhole(b) // no spills
}

// a and a's cleanup
// 2 PUTFIELD .*L\$0 : Ljava/lang/Object;
// 0 PUTFIELD .*L\$1 : Ljava/lang/Object;