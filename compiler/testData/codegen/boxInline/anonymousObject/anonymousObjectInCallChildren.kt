// NO_CHECK_LAMBDA_INLINING

// FILE: utils.kt
fun functionWithLambda(f: (String) -> String): String = f("OK")

fun functionWithLambda(f: (StringWrapper) -> StringWrapper): StringWrapper = f(StringWrapper("OK"))

fun <T> genericFunctionWithLambda(f: () -> T): T = f()

inline fun <T, R> T.testLet(block: (T) -> R): R = block(this)

inline fun <T> T.testApplyToExtensionReceiver(block: T.() -> Unit): T {
    block()
    return this
}

class StringWrapper(val s: String) {
    inline fun testApplyToDispatchReceiver(block: StringWrapper.() -> Unit): String {
        block()
        return s
    }
}

inline fun <T> testApplyToArg0(args: T, block: T.() -> Unit): T {
    args.block()
    return args
}

// FILE: testClass.kt
class TestClass {
    val testExtensionReceiver = functionWithLambda { strArg: String ->
        val anonymousObj = genericFunctionWithLambda {
            strArg.testLet {
                object {
                    val strField = it
                }
            }
        }
        anonymousObj.strField
    }.testApplyToExtensionReceiver {}

    val testDispatchReceiver = functionWithLambda { strArg: StringWrapper ->
        val anonymousObj = genericFunctionWithLambda {
            strArg.testLet {
                object {
                    val strField = it
                }
            }
        }
        anonymousObj.strField
    }.testApplyToDispatchReceiver {}

    val testArg0 = testApplyToArg0(functionWithLambda { strArg: String ->
        val anonymousObj = genericFunctionWithLambda {
            strArg.testLet {
                object {
                    val strField = it
                }
            }
        }
        anonymousObj.strField
    }) {}

    val testChain = functionWithLambda { strArg: String ->
        val anonymousObj = genericFunctionWithLambda {
            strArg.testLet {
                object {
                    val strField1 = it
                }
            }.testLet {
                object {
                    val strField2 = it.strField1
                }
            }
        }
        anonymousObj.strField2
    }.testApplyToExtensionReceiver {}
}

// FILE: main.kt
fun box(): String {
    val testObject = TestClass()
    when {
        testObject.testExtensionReceiver != "OK" -> return "testExtensionReceiver failed"
        testObject.testDispatchReceiver != "OK" -> return "testDispatchReceiver failed"
        testObject.testArg0 != "OK" -> return "testArg0 failed"
        testObject.testChain != "OK" -> return "testChain failed"
        else -> return "OK"
    }
}
