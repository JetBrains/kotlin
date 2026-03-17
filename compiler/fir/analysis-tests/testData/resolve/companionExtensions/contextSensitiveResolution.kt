// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions +ContextSensitiveResolutionUsingExpectedType

class C

<!WRONG_MODIFIER_TARGET!>companion<!> val C.Instance get() = C()

fun test(): C {
    return Instance
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, propertyDeclaration, propertyWithExtensionReceiver */
