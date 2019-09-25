// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE

import kotlin.contracts.*

@ExperimentalContracts
fun runOnce(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    action()
}

@ExperimentalContracts
class Foo(foo: Boolean) {
    var res = "FAIL"
    init {
        runOnce {
            foo
            res = "OK"
        }
    }
}

@ExperimentalContracts
fun box(): String {
    val foo = Foo(true)
    return foo.res
}