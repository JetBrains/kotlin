// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize reified T> getClazz() = T::class.java
fun <@JvmSpecialize reified T> getArrayClazz() = getClazz<Array<T>>()

fun box(): String {
    if (getClazz<Int>().name != "java.lang.Integer") return "fail: Int"
    if (getClazz<String>().name != "java.lang.String") return "fail: String"
    if (getClazz<Array<Int>>().name != "[Ljava.lang.Integer;") return "fail: Array<Int>"
    if (getClazz<IntArray>().name != "[I") return "fail: IntArray"
    if (getArrayClazz<IntArray>().name != "[[I") return "fail (2): IntArray"
    if (getArrayClazz<String>().name != "[Ljava.lang.String;") return "fail (2): String"
    return "OK"
}
