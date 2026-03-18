// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocksAndExtensions
class C1 {
    <!UNSUPPORTED_FEATURE!>companion<!> {}
}

class C2 {
    <!UNSUPPORTED_FEATURE!>companion<!> {
        fun foo() {}
    }
}

class C3 {
    <!UNSUPPORTED_FEATURE!>companion<!> {}
    companion {
        val bar = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
