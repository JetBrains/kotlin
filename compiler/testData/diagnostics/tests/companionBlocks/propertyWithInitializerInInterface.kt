// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-85770
// LANGUAGE: +CompanionBlocksAndExtensions

interface A {
    companion {
        val X = 1
    }
}

/* GENERATED_FIR_TAGS: integerLiteral, interfaceDeclaration, propertyDeclaration */
