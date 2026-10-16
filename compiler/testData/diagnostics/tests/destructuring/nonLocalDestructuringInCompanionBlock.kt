// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-90199

data class X(val a: Int, val b: String)

class A {
    companion {
        val <!SYNTAX!>(a, b)<!> = X(1, "")
    }

    fun use() = b.length
}

fun use() = A.b.length

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, functionDeclaration, integerLiteral,
primaryConstructor, propertyDeclaration, stringLiteral */
