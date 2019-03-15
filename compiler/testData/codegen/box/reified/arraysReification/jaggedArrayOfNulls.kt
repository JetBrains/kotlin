// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

inline fun <reified T> jaggedArrayOfNulls(): Array<Array<T>?> = arrayOfNulls<Array<T>>(1)

fun box(): String {
    val x1 = jaggedArrayOfNulls<String>().javaClass.simpleName
    if (x1 != "String[][]") return "fail1: $x1"

    val x2 = jaggedArrayOfNulls<Array<String>>().javaClass.simpleName
    if (x2 != "String[][][]") return "fail2: $x2"

    val x3 = jaggedArrayOfNulls<IntArray>().javaClass.simpleName
    if (x3 != "int[][][]") return "fail3: $x3"
    return "OK"
}
