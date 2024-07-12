// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = UserKlass().Inner<Placeholder>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass {
    inner class Inner<T>
}
class Placeholder

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass.Inner<Placeholder> = UserKlass().Inner()
    val buildee = build {
        yield(arg)
    }
    checkExactType<Buildee<UserKlass.Inner<Placeholder>>>(buildee)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass.Inner<Placeholder>) {}
    val buildee = build {
        consume(materialize())
    }
    checkExactType<Buildee<UserKlass.Inner<Placeholder>>>(buildee)
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
