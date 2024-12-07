// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-61907
// REASON: unexpected red code in K1 (see corresponding diagnostic test)

fun box(): String {
    testYield()
    return "OK"
}

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    build {
        variable = arg
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    var variable: CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
