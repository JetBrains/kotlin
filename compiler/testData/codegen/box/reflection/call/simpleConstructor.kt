// TARGET_BACKEND: JVM
// WITH_REFLECT

class A(val result: String)

fun box(): String {
    val a = (::A).call("OK")
    return a.result
}
