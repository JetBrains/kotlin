// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = { -> } as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    fun testBasicCase() {
        val arg: () -> Unit = { -> }
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<() -> Unit>>(buildee)
    }

    fun testLiterals() {
        fun test1() {
            val buildee = build {
                yield { -> }
            }
            checkExactType<Buildee<() -> Unit>>(buildee)
        }
        fun test2() {
            val buildee = build {
                yield(fun() {})
            }
            checkExactType<Buildee<() -> Unit>>(buildee)
        }
        fun test3() {
            val buildee = build {
                yield {}
            }
            checkExactType<Buildee<() -> Unit>>(buildee)
        }

        test1()
        test2()
        test3()
    }

    testBasicCase()
    testLiterals()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testBasicCase() {
        fun consume(arg: () -> Unit) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<() -> Unit>>(buildee)
    }

    fun testLiterals() {
        fun test1() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo({ -> }, materialize())
            }
            checkExactType<Buildee<() -> Unit>>(buildee)
        }
        fun test2() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(fun() {}, materialize())
            }
            checkExactType<Buildee<() -> Unit>>(buildee)
        }
        fun test3() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo({}, materialize())
            }
            checkExactType<Buildee<() -> Unit>>(buildee)
        }

        test1()
        test2()
        test3()
    }

    testBasicCase()
    testLiterals()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
