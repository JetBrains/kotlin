// IGNORE_BACKEND_FIR: JVM_IR
abstract class Base(val s: String, vararg ints: Int)

fun foo(s: String, ints: IntArray) = object : Base(ints = *ints, s = s) {}

fun box(): String {
    return foo("OK", intArrayOf(1, 2)).s
}

