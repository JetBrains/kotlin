// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize reified T> mkArray() = emptyArray<T>()
fun <@JvmSpecialize reified T> mkArray2() = emptyArray<Array<T>>()

inline fun <reified T> inlineMkArray() = emptyArray<T>()
inline fun <reified T> inlineMkArray2() = emptyArray<Array<T>>()

fun box(): String {
    if (mkArray<Int>().javaClass !== inlineMkArray<Int>().javaClass) return "fail: for mkArray<Int>"
    if (mkArray2<Int>().javaClass !== inlineMkArray2<Int>().javaClass) return "fail: for mkArray2<Int>"

    if (mkArray<String>().javaClass !== inlineMkArray<String>().javaClass) return "fail: for mkArray<String>"
    if (mkArray2<String>().javaClass !== inlineMkArray2<String>().javaClass) return "fail: for mkArray2<String>"

    if (mkArray<Array<Int>>().javaClass !== inlineMkArray<Array<Int>>().javaClass) return "fail: for mkArray<Array<Int>>"
    if (mkArray2<Array<Int>>().javaClass !== inlineMkArray2<Array<Int>>().javaClass) return "fail: for mkArray2<Array<Int>>"

    if (mkArray<Array<String>>().javaClass !== inlineMkArray<Array<String>>().javaClass) return "fail: for mkArray<Array<String>>"
    if (mkArray2<Array<String>>().javaClass !== inlineMkArray2<Array<String>>().javaClass) return "fail: for mkArray2<Array<String>>"

    return "OK"
}
