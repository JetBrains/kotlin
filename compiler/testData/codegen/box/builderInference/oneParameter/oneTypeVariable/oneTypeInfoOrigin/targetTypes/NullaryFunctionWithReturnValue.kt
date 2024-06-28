// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = l@ { return@l UserKlass() } as CT
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
        val arg: () -> UserKlass = l@ { return@l UserKlass() }
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<() -> UserKlass>>(buildee)
    }

    fun testLiterals() {
        fun test1() {
            val buildee = build {
                yield { return@yield UserKlass() }
            }
            checkExactType<Buildee<() -> UserKlass>>(buildee)
        }
        fun test2() {
            val buildee = build {
                yield(fun(): UserKlass { return UserKlass() })
            }
            checkExactType<Buildee<() -> UserKlass>>(buildee)
        }
        fun test3() {
            val buildee = build {
                yield { UserKlass() }
            }
            checkExactType<Buildee<() -> UserKlass>>(buildee)
        }
        fun test4() {
            val buildee = build {
                yield(fun() = UserKlass())
            }
            checkExactType<Buildee<() -> UserKlass>>(buildee)
        }

        test1()
        test2()
        test3()
        test4()
    }

    testBasicCase()
    testLiterals()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testBasicCase() {
        fun consume(arg: () -> UserKlass) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<() -> UserKlass>>(buildee)
    }

    fun testLiterals() {
        fun test1() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(
                    { return@shareTypeInfo UserKlass() },
                    materialize()
                )
            }
            checkExactType<Buildee<() -> UserKlass>>(buildee)
        }
        fun test2() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(
                    fun(): UserKlass { return UserKlass() },
                    materialize()
                )
            }
            checkExactType<Buildee<() -> UserKlass>>(buildee)
        }
        fun test3() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo({ UserKlass() }, materialize())
            }
            checkExactType<Buildee<() -> UserKlass>>(buildee)
        }
        fun test4() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(fun() = UserKlass(), materialize())
            }
            checkExactType<Buildee<() -> UserKlass>>(buildee)
        }

        test1()
        test2()
        test3()
        test4()
    }

    testBasicCase()
    testLiterals()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
