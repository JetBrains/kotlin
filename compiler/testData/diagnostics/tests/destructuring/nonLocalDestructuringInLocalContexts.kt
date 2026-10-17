// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ISSUE: KT-90199

data class X(val a: Int, val b: String)

enum class E {
    ENTRY {
        val <!SYNTAX!>(a, b)<!> = X(1, "")

        fun use() = b.length
    };
}

fun local() {
    class Local {
        val <!SYNTAX!>(a, b)<!> = X(1, "")

        fun use() = b.length
    }

    Local().b.length

    val o = object {
        val <!SYNTAX!>(a, b)<!> = X(1, "")
    }

    o.b.length
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, data, enumDeclaration, enumEntry,
functionDeclaration, integerLiteral, localClass, localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
