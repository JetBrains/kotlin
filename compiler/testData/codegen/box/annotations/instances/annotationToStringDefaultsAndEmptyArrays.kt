// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM
// IGNORE_BACKEND: ANDROID

annotation class A(val t: String = "d")
annotation class B(
    val a: A = A(),
    val arr: Array<A> = emptyArray()
)

fun box(): String {
    val s = B().toString()

    if (!(s.contains("@B("))) return "Fail1"
    if (!(s.contains("a=@A(t=d)"))) return "Fail2"
    if (!s.contains("arr=[]")) return "Fail3"

    return "OK"
}
