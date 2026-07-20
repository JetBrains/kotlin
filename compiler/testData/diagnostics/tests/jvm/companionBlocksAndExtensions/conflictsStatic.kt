// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB

class A {

    companion object {
        @JvmStatic
        <!CONFLICTING_JVM_DECLARATIONS!>val a: String<!> = "companionObjectVal"
        @JvmStatic
        <!CONFLICTING_JVM_DECLARATIONS!>fun foo(s: String) = "companionObjectFun: $s"<!>
    }
    companion {
        <!CONFLICTING_JVM_DECLARATIONS!>val a: String<!> = "companionBlockVal"
        <!CONFLICTING_JVM_DECLARATIONS!>fun foo(s: String) = "companionBlockFun: $s"<!>
    }

}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, propertyDeclaration,
stringLiteral */
