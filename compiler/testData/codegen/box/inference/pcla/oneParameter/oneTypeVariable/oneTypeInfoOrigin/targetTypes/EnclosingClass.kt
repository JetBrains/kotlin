// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass {
    // test 1: PTV is in consuming position (yield-case)
    fun testYield() {
        fun testBasicCase() {
            val arg: UserKlass = UserKlass()
            val buildee = build {
                yield(arg)
            }
            checkExactType<Buildee<UserKlass>>(buildee)
        }

        fun testThisExpression() {
            val buildee = build {
                yield(this@UserKlass)
            }
            checkExactType<Buildee<UserKlass>>(buildee)
        }

        testBasicCase()
        testThisExpression()
    }

    // test 2: PTV is in producing position (materialize-case)
    fun testMaterialize() {
        fun testBasicCase() {
            fun consume(arg: UserKlass) {}
            val buildee = build {
                consume(materialize())
            }
            checkExactType<Buildee<UserKlass>>(buildee)
        }

        fun testThisExpression() {
            fun <T> shareTypeInfo(from: T, to: T) {}
            val buildee = build {
                shareTypeInfo(this@UserKlass, materialize())
            }
            checkExactType<Buildee<UserKlass>>(buildee)
        }

        testBasicCase()
        testThisExpression()
    }
}

fun box(): String {
    with(UserKlass()) {
        testYield()
        testMaterialize()
    }
    return "OK"
}
