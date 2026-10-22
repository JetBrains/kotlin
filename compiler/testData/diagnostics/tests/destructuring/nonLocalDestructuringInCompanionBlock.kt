// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-90199

data class X(val a: Int, val b: String)

class A {
    companion {
        val <!SYNTAX!>(a, b)<!> = X(1, "")
    }

    fun use() = <!UNRESOLVED_REFERENCE!>b<!>.length
}

fun use() = A.<!UNRESOLVED_REFERENCE!>b<!>.length

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
