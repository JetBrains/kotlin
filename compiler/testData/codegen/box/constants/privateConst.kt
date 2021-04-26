fun <T> eval(fn: () -> T) = fn()

private const val z = "OK";

fun box(): String {
    return eval { z }
}