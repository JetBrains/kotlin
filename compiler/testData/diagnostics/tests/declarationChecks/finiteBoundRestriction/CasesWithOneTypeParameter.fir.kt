interface A0<T : A0<T>>
interface A1<T : A1<*>>
interface A2<T : A2<out T>>
// StackOverflowError
//interface A3<T : A3<in T>>
interface A4<T : A4<*>?>

interface B0<T : B1<*>>
interface B1<T : B0<*>>

interface AA<T: AA<*>>
interface BB<S : List<AA<*>>>

interface A<T: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>List<T, T, T><!>>

class X<Y>
class D<T : X<in X<out X<T>>>>