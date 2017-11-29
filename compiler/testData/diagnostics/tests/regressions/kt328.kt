// !WITH_NEW_INFERENCE
//KT-328 Local function in function literals cause exceptions

fun bar1() = {
    <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!NI;DEBUG_INFO_MISSING_UNRESOLVED!>bar1<!>()<!>
}

fun bar2() = {
    fun foo2() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!NI;DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!>()<!>
}

//properties
//in a class
class A() {
    val x = { <!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;UNINITIALIZED_VARIABLE, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>x<!> }
}

//in a package
val x = { <!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;UNINITIALIZED_VARIABLE, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>x<!> }

//KT-787 AssertionError on code 'val x = x'
val z = <!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;UNINITIALIZED_VARIABLE, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>z<!>

//KT-329 Assertion failure on local function
fun block(f : () -> Unit) = f()

fun bar3() = block{ <!UNRESOLVED_REFERENCE!>foo3<!>() // <-- missing closing curly bracket
fun foo3() = block{ <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!NI;DEBUG_INFO_MISSING_UNRESOLVED!>bar3<!>()<!> }<!SYNTAX!><!>

