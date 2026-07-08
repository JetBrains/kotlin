// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
// ISSUE: KT-87360

interface I {
    companion {
        <!INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD!>val x<!> by lazy { 1 }

        <!INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD!>val y: Int<!> = 0
            get() = field

        <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun foo()<!>

        <!INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD, MUST_BE_INITIALIZED!>val bar: String<!>

        <!INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD!><!PRIVATE_PROPERTY_IN_INTERFACE!>private<!> val private1<!> = 1
        <!INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD!><!PRIVATE_PROPERTY_IN_INTERFACE!>private<!> <!INTERFACE_COMPANION_BLOCK_VAR!>var<!> private2<!> = 2
        <!INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD!><!INTERFACE_COMPANION_BLOCK_VAR!>var<!> private3<!> = 2
            private set

        <!INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD!><!INTERFACE_COMPANION_BLOCK_VAR!>var<!> public<!> = 1

        const val X = 1

        @JvmField
        val Y = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, integerLiteral, interfaceDeclaration, lambdaLiteral,
nullableType, propertyDeclaration, propertyDelegate */
