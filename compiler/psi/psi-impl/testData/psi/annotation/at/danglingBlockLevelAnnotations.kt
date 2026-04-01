// COMPILATION_ERRORS

class C {
    fun test() {
        @Ann
    }

    fun foo() {
        class Local {
            @Ann
        }
    }
    @Ann
}

@Ann
