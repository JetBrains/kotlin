fun box(): String {
    testYield()
    return "OK"
}

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    build {
        yield(it)
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun yield(arg: CT) {}
}

fun <FT> build(
    instructions: Buildee<FT>.(UserKlass) -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply { this.instructions(UserKlass()) }
}

class UserKlass
