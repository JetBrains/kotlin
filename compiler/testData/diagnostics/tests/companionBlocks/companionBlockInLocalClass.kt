// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks
fun foo() {
    class C {
        <!ILLEGAL_COMPANION_BLOCK!>companion<!> { }
    }
    class C2 {
        <!ILLEGAL_COMPANION_BLOCK!>companion<!> {
            fun foo() {}
        }
    }
    <!LOCAL_INTERFACE_NOT_ALLOWED!>interface I<!> {
        <!ILLEGAL_COMPANION_BLOCK!>companion<!> {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, localClass */
