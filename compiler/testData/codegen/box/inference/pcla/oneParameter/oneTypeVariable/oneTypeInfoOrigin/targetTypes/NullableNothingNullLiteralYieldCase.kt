// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun testYield() {
    val buildee = build {
        yield(null)
    }
    checkExactType<Buildee<Nothing?>>(buildee)
}

fun box(): String {
    testYield()
    return "OK"
}
