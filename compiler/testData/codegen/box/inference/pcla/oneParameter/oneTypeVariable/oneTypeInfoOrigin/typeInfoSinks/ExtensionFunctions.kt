fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    build {
        yield(arg)
    }
}

// PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    build {
        consume(materialize())
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT>

fun <EFT> Buildee<EFT>.yield(arg: EFT) {}
fun <EFT> Buildee<EFT>.materialize(): EFT = UserKlass() as EFT

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
