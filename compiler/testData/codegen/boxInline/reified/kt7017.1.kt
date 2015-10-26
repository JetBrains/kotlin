import test.*

fun box(): String {
    if (!test<String>("OK")) return "fail 1"

    if (test<Int>("OK")) return "fail 2"

    return "OK"
}