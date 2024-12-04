// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

package test

interface Z{

    private fun extension(): String {
        return "OK"
    }
}

object Z2 : Z {

}

fun box() : String {
    val size = Class.forName("test.Z2").declaredMethods.size
    if (size != 0) return "fail: $size"
    return "OK"
}