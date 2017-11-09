fun f(a: MutableList<String>) = <!USELESS_IS_CHECK!>a is MutableList<out CharSequence><!>
