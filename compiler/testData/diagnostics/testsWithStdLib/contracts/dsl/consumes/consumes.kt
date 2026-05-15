// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ConsumesContract
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract

inline fun <T : AutoCloseable, R> T.myUse(block: (T) -> R): R {
    contract {
        consumes(this@myUse)
    }
    return try { block(this) } finally { this.close() }
}

class File(val name: String) : AutoCloseable {
    override fun close() { }
}

fun test(x: Any) {
    val file = File("me.txt")
    file.myUse { true }
    val name = <!CONSUMED_VALUE!>file<!>.name
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConsumesEffect, contracts, funWithExtensionReceiver,
functionDeclaration, functionalType, inline, lambdaLiteral, localProperty, nullableType, primaryConstructor,
propertyDeclaration, thisExpression, tryExpression, typeConstraint, typeParameter */
