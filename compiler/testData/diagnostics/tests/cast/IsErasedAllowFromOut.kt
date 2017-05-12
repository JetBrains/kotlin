fun f(a: MutableList<out Number>) = <!USELESS_IS_CHECK!>a is MutableList<out Any><!>
