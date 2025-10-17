// RUN_PIPELINE_TILL: FRONTEND

class ModifierOnType {
    val modifierOnType: suspend () -> Unit
        <!REDUNDANT_EXPLICIT_BACKING_FIELD!>field<!>: suspend () -> Unit = {}

    val modifierOnTypeWithAny: Any
        field: suspend () -> Unit = {}

    fun noSuspendUsage() {
        <!ILLEGAL_SUSPEND_FUNCTION_CALL!>modifierOnType<!>()
        <!ILLEGAL_SUSPEND_FUNCTION_CALL!>modifierOnTypeWithAny<!>()
    }

    suspend fun suspendUsage() {
        modifierOnType()
        modifierOnTypeWithAny()
    }
}

fun noSuspendUsageOutside() {
    ModifierOnType().<!ILLEGAL_SUSPEND_FUNCTION_CALL!>modifierOnType<!>()
    ModifierOnType().modifierOnTypeWithAny()
}

suspend fun suspendUsageOutside() {
    ModifierOnType().modifierOnType()
    ModifierOnType().modifierOnTypeWithAny()
}

operator fun Any.invoke() {}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, funWithExtensionReceiver, functionDeclaration,
functionalType, lambdaLiteral, operator, propertyDeclaration, smartcast, suspend */
