// DUMP_IR_DIFFERENCE: JVM
//   K/JVM rethrows actualized java.lang.Exception instead of kotlin.Exception

fun test(f: () -> Unit) =
        try { f() } catch (e: Exception) { throw e }
