// IGNORE_BACKEND_K2: NATIVE
// IGNORE_BACKEND_K2: WASM
// IGNORE_BACKEND_K2: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-59551

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
    val buildee = build(fun(it) {
        it.yield(arg)
    })
    checkExactType<Buildee<UserKlass>>(buildee)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    val buildee = build(fun(it) {
        consume(it.materialize())
    })
    checkExactType<Buildee<UserKlass>>(buildee)
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
