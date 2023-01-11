// ENABLE_JVM_IR_INLINER
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

package kotlin

inline fun <reified T> foo(p: VArray<T>): String {
    return p.iterator().next().toString()
}


fun box(): String {
    val x = foo(VArray(1) { 42 })

    return "OK"
}