// DIAGNOSTICS: -UNCHECKED_CAST

// CHECK_TYPE_WITH_EXACT

// ISSUE: KT-59551
// (also see an analogous codegen test)

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

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    val buildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!>(fun(it) {
        it.<!UNRESOLVED_REFERENCE!>yield<!>(arg)
    })
    checkExactType<Buildee<UserKlass>>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>buildee<!>)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    val buildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!>(fun(it) {
        consume(it.<!UNRESOLVED_REFERENCE!>materialize<!>())
    })
    checkExactType<Buildee<UserKlass>>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>buildee<!>)
}
