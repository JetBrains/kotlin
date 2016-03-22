annotation class Ann
annotation class Ann2

class C {
    fun foo() {
        class Local {
            @<!UNRESOLVED_REFERENCE!>Ann0<!>
            @Ann @<!UNRESOLVED_REFERENCE!>Ann3<!>
            @Ann2(<!TOO_MANY_ARGUMENTS!>1<!>)
            @<!UNRESOLVED_REFERENCE!>Ann4<!><!SYNTAX!><!>
        }
    }
    @<!UNRESOLVED_REFERENCE!>Ann0<!>
    @Ann @<!UNRESOLVED_REFERENCE!>Ann3<!>
    @Ann2(<!TOO_MANY_ARGUMENTS!>1<!>)
    @<!UNRESOLVED_REFERENCE!>Ann4<!><!SYNTAX!><!>
}

@<!UNRESOLVED_REFERENCE!>Ann0<!>
@Ann @<!UNRESOLVED_REFERENCE!>Ann3<!>
@Ann2(<!TOO_MANY_ARGUMENTS!>1<!>)
@<!UNRESOLVED_REFERENCE!>Ann4<!SYNTAX!><!><!>