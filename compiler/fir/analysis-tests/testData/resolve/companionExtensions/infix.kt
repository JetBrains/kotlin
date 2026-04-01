// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> companion fun C.infix(s: String) {}

fun test() {
    C infix ""
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, infix, stringLiteral */
