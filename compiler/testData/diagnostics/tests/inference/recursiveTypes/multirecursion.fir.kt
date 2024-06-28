// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

interface I1<A : I1<A>>
interface I2<C : I2<C>>

interface I3<E: I3<E>> : I2<E>, I1<E>
interface I4<E: I4<E>> : I2<E>, I1<E>

class C1<F> : I3<C1<F>>
class C2 : I4<C2>

class Box<T>

fun test(c1: C1<Box<Box<Box<Int>>>>, c2: C2) {
    val v = select(c1, c2)
    <!DEBUG_INFO_EXPRESSION_TYPE("I2<*> & I1<*>")!>v<!>
}

fun <S> select(vararg args: S): S = TODO()
