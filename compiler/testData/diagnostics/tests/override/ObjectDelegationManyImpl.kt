interface D {
    fun foo()
}

interface E {
    fun foo() {}
}

object Impl : D, E {
    override fun foo() {}
}

val obj: D = <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>object<!> : D by Impl, E by Impl {}