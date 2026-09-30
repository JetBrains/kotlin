// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-45176
// DUMP_CFG

// FILE: main.kt
class DefiniteInitializationInInitSection {
    val y: Int

    init {
        myRunAfter { <!NON_INLINE_MEMBER_VAL_INITIALIZATION!>y<!> = 43 }
    }
}

class DefiniteInitializationInInitSectionInline {
    val y: Int

    init {
        myInlineRunAfter { y = 43 }
        y.inc()
    }
}

class ReassignmentInInitSectionInline {
    val y: Int

    init {
        myInlineRunAfter { y = 43 }
        <!VAL_REASSIGNMENT!>y<!> = 44
    }
}

fun localInitialization() {
    val x: Int
    myRunAfter { x = 42 }
    x.inc()

    val z: Int
    myInlineRunAfter { z = 42 }
    z.inc()
}

fun localReassignment() {
    val x: Int
    myRunAfter { x = 42 }
    <!VAL_REASSIGNMENT!>x<!> = 43
    x.inc()
}

// FILE: contracts.kt
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun <T> myRunAfter(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

@OptIn(ExperimentalContracts::class)
inline fun <T> myInlineRunAfter(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, classReference, contractCallsEffect, contracts, functionDeclaration,
functionalType, init, inline, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter */
