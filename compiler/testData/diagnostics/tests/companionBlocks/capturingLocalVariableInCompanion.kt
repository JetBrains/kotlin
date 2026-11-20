// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks

fun g(): Int {
    val v = 1;
    class L {
        <!ILLEGAL_COMPANION_BLOCK!>companion<!> {
            fun f() = v
        }
    }
    return L.f()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localClass, localProperty,
propertyDeclaration */
