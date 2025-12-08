// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class A {
    fun funA(): String {
        return "A.funA body"
    }

    inline fun funB(): String {
        return "A.funB body"
    }

    @OptIn(ExperimentalContracts::class)
    fun isNotNull(value: Any?): Boolean {
        contract {
            returns(true) implies (value != null)
        }
        return value != null
    }

    private fun funC(): String {
        return "A.funC body"
    }

    fun funD() = 1 + 2

    inline fun funE(): String {
        fun funF() = "funF body"
        return funF()
    }
}

interface B {
    fun funA(): String {
        return "B.funA body"
    }

    fun funB(): String
}

class C : B {
    inner class D {
        fun funA() : String {
            return "D.fun body"
        }
    }

    override fun funB(): String {
        return "C.funB body"
    }
}

class E {
    fun funA() : String {
        return "E.fun body"
    }
}

class F : E {
    fun funA() : String {
        return "F.fun body"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, contractConditionalEffect, contracts, functionDeclaration,
inline, interfaceDeclaration, nullableType, override, stringLiteral */
