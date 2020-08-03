fun blackhole(vararg a: Any?) {}

suspend fun dummy() {}

fun check(): Int = 1

suspend fun test() {
    when (check()) {
        0 -> {
            val a = ""
            dummy()
            blackhole(a)
        }
        1 -> {
            val a = ""
            val b = ""
            dummy()
            blackhole(a, b)
        }
        else -> {
            val a = ""
            val b = ""
            val c = 1
            dummy()
            blackhole(a, b, c)
        }
    }
    // Cleanup both a and b, but c is primitive, so no need to clean it up
    dummy()
}

// 2 ACONST_NULL
// 4 PUTFIELD .*L\$0 : Ljava/lang/Object;
// 3 PUTFIELD .*L\$1 : Ljava/lang/Object;