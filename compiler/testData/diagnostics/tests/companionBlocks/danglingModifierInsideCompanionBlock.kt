// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        fun t() {
        }
        <!SYNTAX!>for<!> <!SYNTAX!>(<!><!SYNTAX!>a<!> in <!SYNTAX!>1<!><!SYNTAX!>..<!><!SYNTAX!>10<!><!SYNTAX!>)<!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{
            <!UNRESOLVED_REFERENCE!>unresolved<!>
        }<!>
    }

    companion {
        fun t2() {}

        @<!UNRESOLVED_REFERENCE!>Anno<!><!SYNTAX!><!>
    }

    @<!UNRESOLVED_REFERENCE!>Anno2<!>
    <!WRONG_MODIFIER_TARGET!>companion<!><!SYNTAX!><!> {
        fun t3() {}

        @<!UNRESOLVED_REFERENCE!>Anno3<!><!SYNTAX!><!>
    }

    @<!UNRESOLVED_REFERENCE!>Anno4<!><!SYNTAX!><!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
