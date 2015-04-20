deprecated(<error>)</error>
fun foo() {}
deprecated(<error>false</error>)
fun boo() {}

fun far() = <warning descr="[DEPRECATED_SYMBOL] 'foo(): Unit' is deprecated.">foo</warning>()

fun bar() = <warning descr="[DEPRECATED_SYMBOL] 'boo(): Unit' is deprecated.">boo</warning>()

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
