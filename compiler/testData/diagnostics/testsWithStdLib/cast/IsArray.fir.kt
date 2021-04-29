fun f(a: Array<out Number>) = a.isArrayOf<Int>()

fun f1(a: Array<out Number>) = <!USELESS_IS_CHECK!>a is Array<*><!>

fun f2(a: Array<out Number>) = a is Array<Int>
