// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = reference as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

private var reference: Any? = null
val <T> Buildee<T>.typeArgumentValue: T get() = reference as T

class UserKlass

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    fun testLocalClassOutsideBuilderArgument() {
        class Local<T>
        reference = Local<UserKlass>()

        val arg: Local<UserKlass> = Local()
        val buildee = build {
            yield(arg)
        }
        checkExactType<Buildee<Local<UserKlass>>>(buildee)
    }

    fun testLocalClassInsideBuilderArgument() {
        val buildee = build {
            class Local<T> { fun localOnlyFunc(): T = UserKlass() as T }
            reference = Local<UserKlass>()

            val arg: Local<UserKlass> = Local()
            yield(arg)
        }
        val result = buildee.typeArgumentValue.localOnlyFunc()
        checkExactType<UserKlass>(result)
    }

    fun testTypeInfoOriginInsideLocalClass() {
        val buildee = build {
            class Local<T> {
                fun localOnlyFunc(): T = UserKlass() as T

                fun initialize() {
                    reference = Local<UserKlass>()

                    val arg: Local<UserKlass> = Local()
                    yield(arg)
                }
            }
            Local<UserKlass>().initialize()
        }
        val result = buildee.typeArgumentValue.localOnlyFunc()
        checkExactType<UserKlass>(result)
    }

    testLocalClassOutsideBuilderArgument()
    testLocalClassInsideBuilderArgument()
    testTypeInfoOriginInsideLocalClass()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testLocalClassOutsideBuilderArgument() {
        class Local<T>
        reference = Local<UserKlass>()

        fun consume(arg: Local<UserKlass>) {}
        val buildee = build {
            consume(materialize())
        }
        checkExactType<Buildee<Local<UserKlass>>>(buildee)
    }

    fun testLocalClassInsideBuilderArgument() {
        val buildee = build {
            class Local<T> { fun localOnlyFunc(): T = UserKlass() as T }
            reference = Local<UserKlass>()

            fun consume(arg: Local<UserKlass>) {}
            consume(materialize())
        }
        val result = buildee.typeArgumentValue.localOnlyFunc()
        checkExactType<UserKlass>(result)
    }

    fun testTypeInfoOriginInsideLocalClass() {
        val buildee = build {
            class Local<T> {
                fun localOnlyFunc(): T = UserKlass() as T

                fun initialize() {
                    reference = Local<UserKlass>()

                    fun consume(arg: Local<UserKlass>) {}
                    consume(materialize())
                }
            }
            Local<UserKlass>().initialize()
        }
        val result = buildee.typeArgumentValue.localOnlyFunc()
        checkExactType<UserKlass>(result)
    }

    testLocalClassOutsideBuilderArgument()
    testLocalClassInsideBuilderArgument()
    testTypeInfoOriginInsideLocalClass()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
