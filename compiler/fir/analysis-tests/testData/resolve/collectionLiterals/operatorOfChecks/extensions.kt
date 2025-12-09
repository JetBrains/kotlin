// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS

class C {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER("must not have an extension receiver")!>operator<!> fun C.of(vararg ints: Int): C = C()
    }
}

<!INAPPLICABLE_OPERATOR_MODIFIER("must not have an extension receiver")!>operator<!> fun C.of(): C = C()

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration,
objectDeclaration, operator, vararg */
