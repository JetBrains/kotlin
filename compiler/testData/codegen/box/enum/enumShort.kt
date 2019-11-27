// IGNORE_BACKEND_FIR: JVM_IR
enum class Color(val rgb: Int) {
    RED(0xff0000),
    GREEN(0x00ff00),
    BLUE(0x0000ff);
}

fun foo(): Int {
    return Color.RED.rgb + Color.GREEN.rgb + Color.BLUE.rgb
}

fun box() = if (foo() == 0xffffff) "OK" else "Fail: ${foo()}"
