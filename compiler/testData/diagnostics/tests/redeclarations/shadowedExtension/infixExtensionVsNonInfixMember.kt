interface IFoo {
    fun foo(i: Int): Int
    infix fun bar(i: Int): Int
}

infix fun IFoo.foo(i: Int) = i
infix fun IFoo.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>(i: Int) = i