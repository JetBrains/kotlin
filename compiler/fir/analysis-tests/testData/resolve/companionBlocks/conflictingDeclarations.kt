// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
    val <!REDECLARATION!>prop<!> = 1

    companion {
        <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
        val <!REDECLARATION!>prop<!> = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
