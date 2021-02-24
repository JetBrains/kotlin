// See KT-14242
var x = 1
fun box(): String {
    val testArray: Array<String?>? = when (1) {
        x -> null
        else -> arrayOfNulls<String>(0)
    }

    // Must not be NPE here
    val size = testArray?.size

    return size?.toString() ?: "OK"
}
