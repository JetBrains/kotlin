interface A {
    fun test(): String = "A"
}

interface B : A {
    override fun test(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Unit<!> = <!TYPE_MISMATCH!>"B"<!>
}

open class C : A

class D : C(), B
