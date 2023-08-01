// ALLOW_FILES_WITH_SAME_NAMES

// The test infrastructure for Kotlin/Native doesn't allow files with same names.
// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// K2 JS_IR MUTE_REASON: java.lang.NullPointerException at org.jetbrains.kotlin.fir.backend.Fir2IrClassifierStorage.getIrClassSymbol
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6

// Test that if we have two different files with the same name in the same package, KT-54028 doesn't reproduce.

// MODULE: lib

// FILE: cursed.kt

sealed interface LazyGridLayoutInfo {
    fun ok(): String
}

// FILE: cursed.kt

class LazyGridState {
    val layoutInfo: LazyGridLayoutInfo
        get() = EmptyLazyGridLayoutInfo
}

private object EmptyLazyGridLayoutInfo : LazyGridLayoutInfo {
    override fun ok() = "OK"
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return LazyGridState().layoutInfo.ok()
}

