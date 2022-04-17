// WITH_STDLIB

abstract class Foo {
}

// ERROR: Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly
fun Foo.contains(vararg xs: Int) = xs.forEach(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR, TYPE_MISMATCH!>this::<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>contains<!><!>)

fun box(): String {
    return "OK"
}
