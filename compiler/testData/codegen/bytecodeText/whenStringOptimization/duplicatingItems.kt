// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.test.assertEquals

fun foo(x : String) : String {
    when (x) {
        "abc" -> return "abc"
        "efg", "ghi", "abc" -> return "efg_ghi"
        else -> return "other"
    }
}

// 1 LOOKUPSWITCH
