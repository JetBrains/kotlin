// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    val buildee = build {
        class LocalClass {
            init {
                yield(arg)
            }
        }
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(buildee)
}

// PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    val buildee = build {
        class LocalClass {
            init {
                consume(materialize())
            }
        }
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(buildee)
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = null!!
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, functionalType, init, lambdaLiteral,
localClass, localFunction, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter,
typeWithExtension */
