// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = null as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val arg: Nothing? = null
    val buildee = build {
        yield(arg)
    }
    checkExactType<Buildee<Nothing?>>(buildee)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: Nothing?) {}
    val buildee = build {
        consume(materialize())
    }
    checkExactType<Buildee<Nothing?>>(buildee)
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
