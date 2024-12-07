// LL_FIR_DIVERGENCE
// LL reports INFERENCE_ERROR based on implicit invocationKind type or not depending on resolve order
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73392
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

object SampleObject {
    val invocationKind = InvocationKind.EXACTLY_ONCE
}

inline fun case_4(block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, SampleObject.invocationKind)<!> }
    return block()
}
