interface Tr {
    fun foo()
}

class C(d: <!UNSUPPORTED!>dynamic<!>) : Tr by d