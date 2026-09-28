// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-80673

interface I

class C : I by when {
    else -> {
        val config = object : I {}
        config
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, inheritanceDelegation, interfaceDeclaration,
localProperty, propertyDeclaration, whenExpression */
