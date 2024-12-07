fun box(): String {
    testMaterialize()
    return "OK"
}

/* TESTS */

// PTV is in producing position (materialize-case)
fun testMaterialize() {
    val arg: UserKlass = UserKlass()
    build {
        var temp = arg
        temp = materialize()
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
