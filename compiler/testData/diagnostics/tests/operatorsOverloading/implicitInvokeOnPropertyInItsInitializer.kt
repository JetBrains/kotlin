// ISSUE: KT-65576

fun foo(): Int = 0

object Implicit {
    operator fun Any.invoke(): String = "Fail"

    val foo = foo()
}

object Explicit {
    operator fun Any.invoke(): String = "Fail"

    val foo: String = <!DEBUG_INFO_LEAKING_THIS, UNINITIALIZED_VARIABLE!>foo<!>()
}

class Inv<T>(val value: T)

object ImplicitWrapped {
    operator fun Inv<*>.invoke(): Inv<String> = Inv("Fail")

    val foo = Inv(<!DEBUG_INFO_MISSING_UNRESOLVED, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!>foo<!>)()
}

object ImplicitIndirect {
    operator fun Any.invoke(): String = "Fail"

    val foo get() = bar()
    val bar get() = baz()
    val baz get() = foo()
}

fun takeInt(x: Int) {}

fun test() {
    takeInt(Implicit.foo)
    takeInt(<!TYPE_MISMATCH!>Explicit.foo<!>) // should be an error
    takeInt(ImplicitWrapped.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>) // should be an error
    takeInt(<!TYPE_MISMATCH!>ImplicitIndirect.foo<!>) // should be an error
    takeInt(<!TYPE_MISMATCH!>ImplicitIndirect.bar<!>) // should be an error
    takeInt(ImplicitIndirect.baz)
}
