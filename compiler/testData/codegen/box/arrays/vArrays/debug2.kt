// ENABLE_JVM_IR_INLINER
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

package kotlin

inline fun <reified T> foo(p: VArray<T>): String {
    return p.iterator().next().toString()
}

@JvmInline
value class IC(val x: Int)


fun box(): String {
    if (foo(VArray(1) { IC(42) }) != "IC(x=42)") return "Fail"

    return "OK1"
}