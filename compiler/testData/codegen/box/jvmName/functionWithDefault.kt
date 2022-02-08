// TARGET_BACKEND: JVM
// WITH_STDLIB

@JvmName("bar")
fun foo(x: String = (object {}).javaClass.enclosingMethod.name) = x

fun box(): String {
    val f = foo()
    if (f != "bar\$default") return "Fail: $f"
    return "OK"
}
