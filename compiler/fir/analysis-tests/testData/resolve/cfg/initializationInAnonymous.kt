// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidInitializationBeforeDeclarationInAnonymous
// ISSUE: KT-77156

interface I {
    var i: Int?
    var j: Int
}

fun create() = object : I {
    init {
        <!INITIALIZATION_BEFORE_DECLARATION_WARNING!>i<!> = 1
    }
    override var i: Int? = null

    init {
        <!INITIALIZATION_BEFORE_DECLARATION_WARNING!>j<!> = 1
    }
    override var j: Int = 2
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, functionDeclaration, init, integerLiteral,
interfaceDeclaration, nullableType, override, propertyDeclaration */
