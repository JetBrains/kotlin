abstract class ClassEmpty {
    abstract fun foo()
}

interface BaseEmpty {
    fun foo()
}

interface BaseDefault {
    fun foo() {}
}

abstract <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class ClassEmpty_BaseEmpty_BaseDefault<!> : ClassEmpty(), BaseEmpty, BaseDefault
