interface IFoo {
    fun foo(i: Int): Int
    <!INAPPLICABLE_INFIX_MODIFIER!>infix fun bar(i: Int): Int<!>
}

infix fun IFoo.foo(i: Int) = i
infix fun IFoo.bar(i: Int) = i