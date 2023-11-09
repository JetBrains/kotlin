// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT

/* TESTS */

// PTV is in producing position (materialize-case) and passed to an immutable extension property
fun testMaterializeWithImmutableProperty() {
    val buildee = build {
        this.typeInfoSourcingValue
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(buildee)
}

// PTV is in producing position (materialize-case) and passed to a mutable extension property
fun testMaterializeWithMutableProperty() {
    val buildee = build {
        this.typeInfoSourcingVariable
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(buildee)
}

/* REQUIRED DECLARATIONS */

class Buildee<CT>

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

val Buildee<UserKlass>.typeInfoSourcingValue: UserKlass get() = UserKlass()
var Buildee<UserKlass>.typeInfoSourcingVariable: UserKlass
    get() = UserKlass()
    set(value) {}
