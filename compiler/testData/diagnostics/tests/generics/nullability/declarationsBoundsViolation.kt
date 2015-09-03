// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A<T : CharSequence>

fun <S : CharSequence?> foo1(a: A<<!UPPER_BOUND_VIOLATED!>S<!>>) {}

class B1<E : String?> : A<<!UPPER_BOUND_VIOLATED!>E<!>>
class B2<E : CharSequence?> : A<<!UPPER_BOUND_VIOLATED!>E<!>>
class B3<E> : A<<!UPPER_BOUND_VIOLATED!>E<!>>

class B4<E : CharSequence> : A<E>

fun <X : CharSequence, Y1 : X, Y2: Y1?> foo(a: A<X>, b: A<Y1>, c: A<<!UPPER_BOUND_VIOLATED!>Y2<!>>) {}
