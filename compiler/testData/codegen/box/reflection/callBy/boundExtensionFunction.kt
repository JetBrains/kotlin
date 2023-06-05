// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

fun String.extFun(k: String, s: String = "") = this + k + s

fun box(): String {
    val sExtFun = "O"::extFun
    return sExtFun.callBy(mapOf(sExtFun.parameters[0] to "K"))
}
