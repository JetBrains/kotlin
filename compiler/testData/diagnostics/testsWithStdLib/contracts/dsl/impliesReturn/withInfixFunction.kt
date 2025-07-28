// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

infix fun String?.shl(x: String): String? {
    contract {
        (this@shl != null) implies (returnsNotNull())
    }
    return this
}

fun usage(x: String, y: String) {
    (x shl y).length
}

/* GENERATED_FIR_TAGS: equalityExpression, funWithExtensionReceiver, functionDeclaration, infix, lambdaLiteral,
nullableType, thisExpression */
