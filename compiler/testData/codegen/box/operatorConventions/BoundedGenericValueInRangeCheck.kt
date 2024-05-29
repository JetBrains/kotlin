// ISSUE: KT-68495

// IGNORE_BACKEND: JVM_IR, JS, JS_IR, JS_IR_ES6, WASM, NATIVE
// REASON: compile-time failure:
//         java.lang.IllegalStateException
//         Has to be a class T of <root>.BoundedGenericValueInRangeCheckKt.test
//         @ org.jetbrains.kotlin.backend.common.lower.loops.UtilsKt.castIfNecessary(Utils.kt:187)

fun <T: Char> test(arg: T) {
    arg in 'A'..'Z'
}

fun box(): String = "OK"
