// CHECK_TYPE_WITH_EXACT

open class A
open class B: A()
class C: B()

fun test(expr: B) {
    checkExactType<A>(<!ARGUMENT_TYPE_MISMATCH!>expr<!>)
    checkExactType<B>(expr)
    checkExactType<C>(<!ARGUMENT_TYPE_MISMATCH!>expr<!>)
    checkTypeEquality(A(), <!ARGUMENT_TYPE_MISMATCH!>expr<!>)
    checkTypeEquality(B(), expr)
    checkTypeEquality(C(), <!ARGUMENT_TYPE_MISMATCH!>expr<!>)
}
