// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_REFLECT

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize reified T> specTypeOf() = kotlin.reflect.typeOf<T>()
fun <@JvmSpecialize T> typeOfUnreifiedPair() = specTypeOf<Pair<T, T>>()

fun box(): String {
    val intPair = typeOfUnreifiedPair<Int>().toString()
    if (intPair != "kotlin.Pair<T, T>") return "fail: $intPair != kotlin.Pair<T, T>"
    return "OK"
}
