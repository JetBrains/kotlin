// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-67013
class ClientResetRequiredException constructor() : Throwable(message = "", cause = null)