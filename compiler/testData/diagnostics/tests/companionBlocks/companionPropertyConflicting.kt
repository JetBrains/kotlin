// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        val <!REDECLARATION!>Baz<!> = 1
    }

    class <!REDECLARATION!>Baz<!>
}

enum class E {
    <!REDECLARATION!>foo<!>;

    companion {
        val <!REDECLARATION!>foo<!> = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nestedClass, propertyDeclaration */
