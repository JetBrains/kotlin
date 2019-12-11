annotation class Ann
annotation class Ann2

class C {
    fun foo() {
        class Local {
            @Ann0
            @Ann @Ann3
            @Ann2(1)
            @Ann4<!SYNTAX!><!>
        }
    }
    @Ann0
    @Ann @Ann3
    @Ann2(1)
    @Ann4<!SYNTAX!><!>
}

@Ann0
@Ann @Ann3
@Ann2(1)
@Ann4<!SYNTAX!><!>