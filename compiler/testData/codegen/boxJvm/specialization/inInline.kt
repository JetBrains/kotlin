// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_REFLECT

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize reified T> typeOfPair() = kotlin.reflect.typeOf<Pair<T, T?>>()
inline fun <reified T> typeOfPairArray() = typeOfPair<Array<T>>()

fun box(): String {
    if (typeOfPairArray<Int>().toString() != "kotlin.Pair<kotlin.Array<kotlin.Int>, kotlin.Array<kotlin.Int>?>") return "fail"
    return "OK"
}
