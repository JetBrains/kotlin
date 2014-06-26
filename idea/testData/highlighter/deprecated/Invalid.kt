deprecated(<error>)</error>
fun foo() {}
deprecated(<error>false</error>)
fun boo() {}

fun far() = <warning descr="'fun foo()' is deprecated.">foo</warning>()

fun bar() = <warning descr="'fun boo()' is deprecated.">boo</warning>()

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
