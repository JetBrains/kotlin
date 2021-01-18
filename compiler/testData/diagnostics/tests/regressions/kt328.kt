// !WITH_NEW_INFERENCE
//KT-328 Local function in function literals cause exceptions

fun bar1() = {
    <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!DEBUG_INFO_MISSING_UNRESOLVED!>bar1<!>()<!>
}

fun bar2() = {
    fun foo2() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!>()<!>
}

//properties
//in a class
class A() {
    val x = { <!DEBUG_INFO_MISSING_UNRESOLVED{NI}, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, UNINITIALIZED_VARIABLE{OI}!>x<!> }
}

//in a package
val x = { <!DEBUG_INFO_MISSING_UNRESOLVED{NI}, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, UNINITIALIZED_VARIABLE{OI}!>x<!> }

//KT-787 AssertionError on code 'val x = x'
val z = <!DEBUG_INFO_MISSING_UNRESOLVED{NI}, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, UNINITIALIZED_VARIABLE{OI}!>z<!>

//KT-329 Assertion failure on local function
fun block(f : () -> Unit) = f()

fun bar3() = block{ <!UNRESOLVED_REFERENCE!>foo3<!>() // <-- missing closing curly bracket
fun foo3() = block{ <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!DEBUG_INFO_MISSING_UNRESOLVED!>bar3<!>()<!> }<!SYNTAX!><!>

