// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions

class C {
    companion {
        <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun infix(s: String) {}
    }
}

fun test() {
    C infix ""
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, infix, stringLiteral */
