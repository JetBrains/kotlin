// !WITH_NEW_INFERENCE
//KT-328 Local function in function literals cause exceptions

fun bar1() = {
    bar1()
}

fun bar2() = {
    fun foo2() = bar2()
}

//properties
//in a class
class A() {
    val x = { x }
}

//in a package
val x = { x }

//KT-787 AssertionError on code 'val x = x'
val z = z

//KT-329 Assertion failure on local function
fun block(f : () -> Unit) = f()

fun bar3() = block{ <!UNRESOLVED_REFERENCE!>foo3<!>() // <-- missing closing curly bracket
fun foo3() = block{ bar3() }<!SYNTAX{PSI}!><!>

