// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// Public function
fun funA(): String {
    return "funA body"
}

// Inline function
inline fun funB(): String {
    return "funB body"
}

// Function with contract
@OptIn(ExperimentalContracts::class)
fun isNotNull(value: Any?): Boolean {
    contract {
        returns(true) implies (value != null)
    }
    return value != null
}

// Private function
private fun funC(): String {
    return "funC body"
}

// Implicit return type
fun funD() = 1 + 2

// Function inside a function
inline fun funE(): String {
    fun funF(): String {
        return "funF body"
    }
    return funF()
}

// Class inside a function
inline fun funG(): String {
    class classA {
        fun funH() = "funH body"
    }
    val a = classA()
    return a.funH()
}

// Implicit type reference from another function.
fun funI() = funD()

fun funJ(): String {
    inline fun funK() = "funK body"
    return funK()
}

/* GENERATED_FIR_TAGS: classReference, contractConditionalEffect, contracts, functionDeclaration, inline, nullableType,
stringLiteral */
