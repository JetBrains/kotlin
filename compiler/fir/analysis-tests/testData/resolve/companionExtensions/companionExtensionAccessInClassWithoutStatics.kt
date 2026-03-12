// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    fun test() {
        foo()
    }
}

<!WRONG_MODIFIER_TARGET!>companion<!> fun C.foo() = ""

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
