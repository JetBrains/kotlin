trait D {
    fun foo()
}

trait E {
    fun foo() {}
}

object Impl : D, E {
    override fun foo() {}
}

val obj: D = <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>object<!> : D by Impl, E by Impl {}
