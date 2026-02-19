// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73585

class Foo

interface Bar {
    fun buzz()
}

val a = object : Bar {
    override fun buzz() {
        super.<!ABSTRACT_SUPER_CALL!>buzz<!>()
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, interfaceDeclaration, override,
propertyDeclaration, superExpression */
