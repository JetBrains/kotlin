// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
class C1
class C2
class E

class A {
    companion {
        <!COMPANION_BLOCK_MEMBER_EXTENSION!>fun E.foo(p1: String, p2: String = p1): String<!> = p2
        <!COMPANION_BLOCK_MEMBER_EXTENSION!>val E.a: Int<!> = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD!>42<!>

        <!COMPANION_BLOCK_MEMBER_EXTENSION!>context(c1: C1, c2: C2)
        fun E.bar(p1: String, p2: String = p1): String<!> = p2
        <!COMPANION_BLOCK_MEMBER_EXTENSION!><!CONTEXT_PARAMETERS_WITH_BACKING_FIELD!>context<!>(c1: C1, c2: C2)
        val E.b: Int<!> = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD!>42<!>

        <!COMPANION_BLOCK_MEMBER_EXTENSION!>context(c1: C1, c2: C2)
        fun C2.foo(p1: String, p2: String = p1): String<!> = p2
        <!COMPANION_BLOCK_MEMBER_EXTENSION!><!CONTEXT_PARAMETERS_WITH_BACKING_FIELD!>context<!>(c1: C1, c2: C2)
        val C2.a: Int<!> = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD!>42<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext */
