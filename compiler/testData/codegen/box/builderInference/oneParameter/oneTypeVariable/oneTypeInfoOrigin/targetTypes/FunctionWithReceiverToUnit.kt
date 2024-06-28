// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = fun UserKlass.() {} as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    fun testBasicCase() {
        val arg: UserKlass.() -> Unit = fun UserKlass.() {}
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<UserKlass.() -> Unit>>(buildee)
    }

    fun testLiteralWithExplicitlyPresentReceiver() {
        val buildee = build {
            yield(fun UserKlass.() {})
        }
        checkExactType<Buildee<UserKlass.() -> Unit>>(buildee)
    }

    testBasicCase()
    testLiteralWithExplicitlyPresentReceiver()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testBasicCase() {
        fun consume(arg: UserKlass.() -> Unit) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<UserKlass.() -> Unit>>(buildee)
    }

    fun testLiteralWithExplicitlyPresentReceiver() {
        fun <T> shareTypeInfo(from: T, to: T) {}
        val buildee = build {
            shareTypeInfo(fun UserKlass.() {}, materialize())
        }
        checkExactType<Buildee<UserKlass.() -> Unit>>(buildee)
    }

    testBasicCase()
    testLiteralWithExplicitlyPresentReceiver()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
