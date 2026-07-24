// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions +CollectionLiterals

class C {
    companion {
        <!COMPANION_BLOCK_MEMBER_EXTENSION!>fun String.foo()<!> {}
        <!COMPANION_BLOCK_MEMBER_EXTENSION!>val String.bar<!> get() = 1
        <!COMPANION_BLOCK_MEMBER_EXTENSION!>val String.baz<!> = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD!>1<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
propertyDeclaration, propertyWithExtensionReceiver */
