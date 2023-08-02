// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = { _: UserKlass -> } as CT
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
        val arg: (UserKlass) -> Unit = { _: UserKlass -> }
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<(UserKlass) -> Unit>>(buildee)
    }

    fun testExplicitlyUnaryLiterals() {
        fun test1() {
            val buildee = build {
                yield { _: UserKlass -> }
            }
            checkExactType<Buildee<(UserKlass) -> Unit>>(buildee)
        }
        fun test2() {
            val buildee = build {
                yield(fun(_: UserKlass) {})
            }
            checkExactType<Buildee<(UserKlass) -> Unit>>(buildee)
        }

        test1()
        test2()
    }

    testBasicCase()
    testExplicitlyUnaryLiterals()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testBasicCase() {
        fun consume(arg: (UserKlass) -> Unit) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<(UserKlass) -> Unit>>(buildee)
    }

    fun testExplicitlyUnaryLiterals() {
        fun test1() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo({ _: UserKlass -> }, materialize())
            }
            checkExactType<Buildee<(UserKlass) -> Unit>>(buildee)
        }
        fun test2() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(fun(_: UserKlass) {}, materialize())
            }
            checkExactType<Buildee<(UserKlass) -> Unit>>(buildee)
        }

        test1()
        test2()
    }

    testBasicCase()
    testExplicitlyUnaryLiterals()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
