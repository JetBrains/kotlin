// ISSUE: KT-65576

fun foo(): Int = 0

object Implicit {
    operator fun Any.invoke(): String = "Fail"

    val foo = foo()
}

object Explicit {
    operator fun Any.invoke(): String = "Fail"

    val foo: String = foo()
}

class Inv<T>(val value: T)

object ImplicitWrapped {
    operator fun Inv<*>.invoke(): Inv<String> = Inv("Fail")

    val foo = <!CANNOT_INFER_PARAMETER_TYPE!>Inv<!>(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo<!>)()
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
    takeInt(<!ARGUMENT_TYPE_MISMATCH!>Explicit.foo<!>) // should be an error
    takeInt(<!ARGUMENT_TYPE_MISMATCH!>ImplicitWrapped.foo<!>) // should be an error
    takeInt(<!ARGUMENT_TYPE_MISMATCH!>ImplicitIndirect.foo<!>) // should be an error
    takeInt(<!ARGUMENT_TYPE_MISMATCH!>ImplicitIndirect.bar<!>) // should be an error
    takeInt(ImplicitIndirect.baz)
}
