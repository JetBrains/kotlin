fun box(): String {
    testMaterialize()
    return "OK"
}

/* TESTS */

// PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    build {
        consume(value)
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT>

val <EFT> Buildee<EFT>.value: EFT get() = UserKlass() as EFT

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
