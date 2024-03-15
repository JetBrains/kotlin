// ISSUE: KT-61633

interface A<T1>
interface B<T2>
interface C<T3>
interface D<T4>

val <X1> A<X1>.provideDelegate: B<X1> get() = TODO()
operator fun <X2> B<X2>.invoke(x: Any?, y: Any?): C<X2> = TODO()

val <X3> C<X3>.getValue: D<X3> get() = TODO()
operator fun <X4> D<X4>.invoke(x: Any?, y: Any?): X4 = TODO()

fun foo(a: A<String>, c: C<String>) {
    val x1 by <!NOT_FUNCTION_AS_OPERATOR!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x1<!>
    x1.length

    val y1 by <!NOT_FUNCTION_AS_OPERATOR!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>y1<!>
    y1.length
}
