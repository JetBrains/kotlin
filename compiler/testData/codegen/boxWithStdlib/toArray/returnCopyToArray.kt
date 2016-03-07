// WITH_RUNTIME

import java.util.Arrays

fun getCopyToArray(): Array<Int> = Arrays.asList(2, 3, 9).toTypedArray()

fun box(): String {
    val str = Arrays.toString(getCopyToArray())
    if (str != "[2, 3, 9]") return str

    return "OK"
}
