// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
// ISSUE: KT-87360

interface I {
    companion {
        val x by lazy { 1 }

        val y: Int = 0
            get() = field

        <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun foo()<!>

        <!MUST_BE_INITIALIZED!>val bar: String<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, integerLiteral, interfaceDeclaration, lambdaLiteral,
nullableType, propertyDeclaration, propertyDelegate */
