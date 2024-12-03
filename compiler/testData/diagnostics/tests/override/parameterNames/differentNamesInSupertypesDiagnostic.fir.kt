// RUN_PIPELINE_TILL: BACKEND
interface C {
    fun foo(a : Int)
}

interface D {
    fun foo(b : Int)
}

interface E : C, D

interface F : C, D {
    override fun foo(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>a<!> : Int) {
        throw UnsupportedOperationException()
    }
}
