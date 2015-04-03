import test.*

fun box(): String {
    if (Z().run() != "null0") return "fail 1: ${Z().run()}"

    if (Z().run("OK") != "OK0") return "fail 2"

    if (Z().run("OK", { a, b -> a + b }, 1) != "OK1") return "fail 3"

    if (Z().run(lambda = { a: String, b: Int -> a + b }) != "0") return "fail 4"

    return "OK"
}
