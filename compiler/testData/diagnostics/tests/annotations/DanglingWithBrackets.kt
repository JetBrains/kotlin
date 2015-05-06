annotation class Ann

class C {
    fun test() {
        @Ann<!SYNTAX!><!>
    }

    fun foo() {
        class Local {
            @Ann<!SYNTAX!><!>
        }
    }
    @Ann<!SYNTAX!><!>
}

@Ann<!SYNTAX!><!>