fun box(): String {
    testYield()
    return "OK"
}

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    build {
        it.yield(this)
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun yield(arg: CT) {}
}

fun <FT> build(
    instructions: UserKlass.(Buildee<FT>) -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply { UserKlass().instructions(this) }
}

class UserKlass
