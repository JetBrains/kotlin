fun blackhole(vararg a: Any?) {}

suspend fun dummy() {}

fun check(): Boolean = true

suspend fun test() {
    if (check()) {
        val a = ""
        dummy()
        blackhole(a)
    } else {
        val a = ""
        val b = ""
        dummy()
        blackhole(a, b)
    }
    // Cleanup both a and b, since the compiler does not know, which branch is going to executed
    dummy()
}

// 2 ACONST_NULL
// 3 PUTFIELD .*L\$0 : Ljava/lang/Object;
// 2 PUTFIELD .*L\$1 : Ljava/lang/Object;