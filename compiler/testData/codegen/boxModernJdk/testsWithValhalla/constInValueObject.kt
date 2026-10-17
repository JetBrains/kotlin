// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses

value object Constants {
    const val X = 1
}

fun box(): String {
    if (!Constants::class.java.isValue) return "Constants is not a value class"
    return if (Constants.X == 1) "OK" else "Fail: ${Constants.X}"
}
