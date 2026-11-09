// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks
// OPT_IN: kotlin.contracts.ExperimentalContracts
import kotlin.contracts.*

class Owner {
    companion {
        fun requireNotNull(x: Any?) {
            contract { returns() implies (x != null) }
            if (x == null) throw IllegalArgumentException()
        }
    }

    class Nested {
        companion {
            fun isString(x: Any?): Boolean {
                contract { returns(true) implies (x is String) }
                return x is String
            }
        }
    }
}

interface I {
    companion {
        fun runOnce(block: () -> Unit) {
            contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
            block()
        }
    }
}

class Outer<T> {
    companion {
        fun isInner(x: Any?): Boolean {
            contract { returns(true) implies (x is <!NO_TYPE_ARGUMENTS_ON_RHS!>Inner<!>) }
            return x is Outer<*>.Inner
        }
    }

    inner class Inner
}

val anonymousObject = object {
    <!ILLEGAL_COMPANION_BLOCK!>companion<!> {
        fun requireNotNull(x: Any?) {
            <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (x != null) }
            if (x == null) throw IllegalArgumentException()
        }
    }

    inner class Inner {
        <!ILLEGAL_COMPANION_BLOCK!>companion<!> {
            fun isString(x: Any?): Boolean {
                <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(true) implies (x is String) }
                return x is String
            }
        }
    }

    fun test(a: String?, b: Any?) {
        requireNotNull(a)
        a.length
        if (Inner.isString(b)) b.length
    }
}

fun test(a: String?, b: Any?) {
    Owner.requireNotNull(a)
    a.length
    if (Owner.Nested.isString(b)) b.length
    val x: Int
    I.runOnce { x = 1 }
    x.inc()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, classDeclaration, contractCallsEffect,
contractConditionalEffect, contracts, equalityExpression, functionDeclaration, functionalType, ifExpression, inner,
integerLiteral, interfaceDeclaration, isExpression, lambdaLiteral, localClass, localProperty, nestedClass, nullableType,
propertyDeclaration, smartcast, starProjection, typeParameter */
