annotation class Foo(val b: Byte)

@Foo('8'.toByte())
const val x = '8'.toByte()

fun box(): String {
    return if (x == 56.toByte()) {
        "OK"
    } else {
        "FAIL"
    }
}