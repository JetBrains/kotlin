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
        class LocalClass {
            init {
                yield(arg)
            }
        }
    }
}

// PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    build {
        class LocalClass {
            init {
                consume(materialize())
            }
        }
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
