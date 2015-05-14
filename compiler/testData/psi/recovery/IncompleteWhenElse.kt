class C {
    fun foo() {
        when {
            1 -> foo()
            else { doIt() }
    }

    fun bar(){}
}}
