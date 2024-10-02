// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// Reason: partial linkage doesn't support friend modules in MPP tests: KT-71949
// JVM_ABI_K1_K2_DIFF: KT-62908

// MODULE: common
// FILE: commonMain.kt
internal annotation class AnnInCommon

// MODULE: friend
// FILE: friendMain.kt
internal annotation class AnnInFriend

// MODULE: main()(friend)(common)
// FILE: main.kt
@AnnInCommon
@AnnInFriend
class A

fun box() = "OK"
