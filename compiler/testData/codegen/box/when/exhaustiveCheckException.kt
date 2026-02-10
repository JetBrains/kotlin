// TARGET_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ISSUE: KT-64652
// FILE: lib.kt
sealed class C {
    class X : C()

    class Y : C()
}

enum class E {
    X, Y
}

inline fun createWrongC(): C = js("{ name: 'Z' }").unsafeCast<C>()

inline fun createWrongE(): E = js("{ name: 'Z' }").unsafeCast<E>()

// FILE: main.kt
fun <T> checkThrown(x: T, block: (T) -> Any?): Unit? {
    return try {
        block(x)
        null
    }
    catch (e: NoWhenBranchMatchedException) {
        Unit
    }
}

fun <T> checkNotThrown(x: T, block: (T) -> Any?): Unit? {
    return try {
        block(x)
        Unit
    }
    catch (e: NoWhenBranchMatchedException) {
        null
    }
}

fun box(): String {
    checkThrown(createWrongC()) {
        when (it) {
            is C.X -> 0
            is C.Y -> 1
        }
    } ?: return "fail1"

    checkNotThrown(createWrongC()) {
        when (it) {
            is C.X -> 0
            else -> 1
        }
    } ?: return "fail2"

    checkThrown(createWrongE()) {
        when (it) {
            E.X -> 0
            E.Y -> 1
        }
    } ?: return "fail3"

    checkNotThrown(createWrongE()) {
        when (it) {
            E.X -> 0
            else -> 1
        }
    } ?: return "fail4"

    checkNotThrown(createWrongC()) {
        when (it) {
            is C.X -> {}
            is C.Y -> {}
        }
        Unit
    } ?: return "fail5"

    checkNotThrown(createWrongC()) {
        when (it) {
            is C.X -> {}
            is C.Y -> {}
        }
    } ?: return "fail6"

    checkNotThrown(createWrongC()) {
        when (it) {
            is C.X -> Unit
            is C.Y -> Unit
        }
    } ?: return "fail7"

    checkThrown(createWrongC()) {
        when (it) {
            is C.X -> Unit
            is C.Y -> null as Unit?
        }
    } ?: return "fail8"

    return "OK"
}
