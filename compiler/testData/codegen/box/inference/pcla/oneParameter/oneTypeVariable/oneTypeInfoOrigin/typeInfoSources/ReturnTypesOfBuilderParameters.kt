fun box(): String {
    testLastStatementReturn()
    testExplicitReturn()
    return "OK"
}

/* TESTS */

// PTV is returned as a last statement of the builder argument
fun testLastStatementReturn() {
    build {
        materialize()
    }
}

// PTV is returned explicitly
fun testExplicitReturn() {
    build {
        return@build materialize()
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> UserKlass
): Buildee<FT> {
    return Buildee<FT>().apply { this.instructions() }
}

class UserKlass
