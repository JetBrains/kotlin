// IGNORE_BACKEND: JVM
/* ^ code compiled by legacy JVM backend fails in run-time with
 * NoSuchMethodError: GenericLocalClassKt$testYield$3$buildee$1$Local: method <init>()V not found
 */

// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

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
