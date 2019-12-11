// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A<T : CharSequence>

fun <S : CharSequence?> foo1(a: A<S>) {}

class B1<E : String?> : A<E>
class B2<E : CharSequence?> : A<E>
class B3<E> : A<E>

class B4<E : CharSequence> : A<E>

fun <X : CharSequence, Y1 : X, Y2: Y1?> foo(a: A<X>, b: A<Y1>, c: A<Y2>) {}
