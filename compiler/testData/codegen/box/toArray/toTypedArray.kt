// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import java.util.Arrays

fun box(): String {
    val array = Arrays.asList(2, 3, 9).toTypedArray()
    if (!array.isArrayOf<Int>()) return array.javaClass.toString()

    val str = Arrays.toString(array)
    if (str != "[2, 3, 9]") return str

    return "OK"
}
