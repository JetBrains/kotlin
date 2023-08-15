// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -DEPRECATION -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

fun <R1> build(block: TestInterface<R1>.() -> Unit): R1 = TODO()

fun test(a: String?) {
    val ret1 = build {
        emit(1)
        get()<!UNNECESSARY_SAFE_CALL!>?.<!>equals("")
    }
}
