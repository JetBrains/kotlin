typealias summatorType = (Int, Int) -> Int

fun summator(x: Int, y: Int) = x + y

fun applier(body: summatorType) = body(11, 22)

fun box(): String {
    if (applier(::summator) == 42) throw AssertionError()

    val t = applier { x, y -> y - x }
    if (t == 11) {
        return "OK"
    } else {
        throw AssertionError()
    }
}