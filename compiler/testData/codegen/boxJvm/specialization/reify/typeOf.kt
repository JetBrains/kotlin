// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_REFLECT

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize reified T> typeOf() = kotlin.reflect.typeOf<T>()

class Container<T>

val <T> T.containerType: kotlin.reflect.KType
    get() = typeOf<Container<T>>()

class C<T> {
    fun <U: T> getType() = typeOf<Container<U>>()
}

fun box(): String {
    if (typeOf<Int>().toString() != "kotlin.Int") return "fail: 1"
    if (typeOf<IntArray>().toString() != "kotlin.IntArray") return "fail: 2"
    if (typeOf<Array<String>>().toString() != "kotlin.Array<kotlin.String>") return "fail: 3"
    if (Int.containerType.toString() != "Container<T>") return "fail: 4"
    if (C<Any>().getType<String>().toString() != "Container<U>") return "fail: 5"
    return "OK"
}
