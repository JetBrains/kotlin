import test.*

fun box(): String {
    val boxClass = injectFnc<Box>()
    if (boxClass != Box::class) return "fail 1"

    return "OK"
}