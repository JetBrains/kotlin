// CHECK_TYPE_WITH_EXACT

open class A
open class B: A()
class C: B()

fun test(expr: B) {
    checkExactType<A>(<!TYPE_MISMATCH!>expr<!>)
    checkExactType<B>(expr)
    checkExactType<C>(<!TYPE_MISMATCH!>expr<!>)
    checkTypeEquality(A(), <!TYPE_MISMATCH!>expr<!>)
    checkTypeEquality(B(), expr)
    checkTypeEquality(C(), <!TYPE_MISMATCH!>expr<!>)
}
