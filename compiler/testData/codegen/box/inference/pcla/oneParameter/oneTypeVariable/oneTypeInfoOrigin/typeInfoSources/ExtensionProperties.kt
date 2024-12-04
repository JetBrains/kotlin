fun box(): String {
    testMaterializeWithImmutableProperty()
    testMaterializeWithMutableProperty()
    return "OK"
}

/* TESTS */

// PTV is in producing position (materialize-case) and passed to an immutable extension property
fun testMaterializeWithImmutableProperty() {
    build {
        this.typeInfoSourcingValue
    }
}

// PTV is in producing position (materialize-case) and passed to a mutable extension property
fun testMaterializeWithMutableProperty() {
    build {
        this.typeInfoSourcingVariable
    }
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
