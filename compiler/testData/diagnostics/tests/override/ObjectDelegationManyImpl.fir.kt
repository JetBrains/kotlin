interface D {
    fun foo()
}

interface E {
    fun foo() {}
}

object Impl : D, E {
    override fun foo() {}
}

val obj: D = <!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>object<!> : D by Impl, E by Impl {}
