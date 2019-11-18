// IGNORE_BACKEND_FIR: JVM_IR
package array_test

fun box() : String {
    var array : IntArray? = IntArray(10)
    array?.set(0, 3)
    if(array?.get(0) != 3) return "fail"

    var a = arrayOfNulls<Array<String?>>(5)
    var b = arrayOfNulls<String>(1)
    b.set(0, "239")
    a?.set(0, b)

    if(a?.get(0)?.get(0) != "239") return "fail"

    return "OK"
}
