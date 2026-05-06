// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LATEST_LV_DIFFERENCE

class C {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo() {}<!>
    <!CONFLICTING_JVM_DECLARATIONS!>val prop<!> = 1

    companion {
        <!CONFLICTING_JVM_DECLARATIONS!>fun foo() {}<!>
        <!CONFLICTING_JVM_DECLARATIONS!>val prop<!> = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
