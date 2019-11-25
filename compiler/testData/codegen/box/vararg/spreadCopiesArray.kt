// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.test.*

fun <T> copyArray(vararg data: T): Array<out T> = data

inline fun <reified T> reifiedCopyArray(vararg data: T): Array<out T> = data

fun copyIntArray(vararg data: Int): IntArray = data

fun box(): String {
    val sarr = arrayOf("OK")
    val sarr2 = copyArray(*sarr)
    sarr[0] = "Array was not copied"
    assertEquals(sarr2[0], "OK", "Failed: Array<String>")

    var rsarr = arrayOf("OK")
    var rsarr2 = reifiedCopyArray(*rsarr)
    rsarr[0] = "Array was not copied"
    assertEquals(rsarr2[0], "OK", "Failed: Array<String>, reified copy")

    val iarr = IntArray(1)
    iarr[0] = 1
    val iarr2 = copyIntArray(*iarr)
    iarr[0] = 42
    assertEquals(iarr2[0], 1, "Failed: IntArray")

    return "OK"
}
