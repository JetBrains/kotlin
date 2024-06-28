fun box(): String {
    testMaterialize()
    return "OK"
}

/* TESTS */

// PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    build {
        consume(variable)
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT>

var <EFT> Buildee<EFT>.variable: EFT
    get() = UserKlass() as EFT
    set(value) {}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
