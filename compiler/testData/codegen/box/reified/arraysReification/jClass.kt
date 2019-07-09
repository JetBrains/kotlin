// TARGET_BACKEND: JVM

// WITH_RUNTIME

inline fun <reified T : Any> jClass() = T::class.java
inline fun <reified T : Any> jClassArray() = jClass<Array<T>>()

fun box(): String {
    if (jClass<Array<String>>().simpleName != "String[]") return "fail 1"
    if (jClass<IntArray>().simpleName != "int[]") return "fail 2"

    if (jClassArray<String>().simpleName != "String[]") return "fail 3"
    if (jClassArray<Array<String>>().simpleName != "String[][]") return "fail 4"
    if (jClassArray<IntArray>().simpleName != "int[][]") return "fail 5"

    return "OK"
}
