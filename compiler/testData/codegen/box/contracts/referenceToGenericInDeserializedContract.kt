// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// ISSUE: KT-76301
// IGNORE_NATIVE: compatibilityTestMode=OldArtifactNewCompiler

// MODULE: lib
// FILE: lib.kt
import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
inline fun <T, reified R> Refinement<T, R>.validate(value: T): Boolean {
    contract {
        returns() implies (value is R)
    }

    return isValid(value)
}

class Refinement<T, R> {
    fun isValid(value: T): Boolean {
        return value is String
    }
}

fun test_fromSource(r: Refinement<Any, String>, x: Any): String {
    r.validate(x)
    return x
}

// MODULE: app(lib)
// FILE: app.kt
fun test_fromLib(r: Refinement<Any, String>, x: Any): String {
    r.validate(x)
    return x
}

fun box(): String {
    val r = Refinement<Any, String>()
    return test_fromSource(r, "O") + test_fromLib(r, "K")
}

