// DIAGNOSTICS: -UNUSED_TYPEALIAS_PARAMETER

interface I1<S>
interface Out<out T>
interface InvOut<T1, out T2>

typealias A1<S> = I1<S>
typealias A2<T, S> = I1<S>
typealias AOut<T> = Out<T>
typealias AInvOut<T1, T2> = InvOut<T1, T2>
typealias AInvOutTT<T> = AInvOut<T, T>

class Test1<out S> : A1<<!TYPE_VARIANCE_CONFLICT_ERROR!>S<!>>
class Test2<out S> : A2<<!TYPE_VARIANCE_CONFLICT_ERROR!>Any<!>, S>
class Test3<out S> : AOut<S>
class Test4<out S> : AInvOut<<!TYPE_VARIANCE_CONFLICT_ERROR!>S<!>, S>
class Test5<out S> : AInvOutTT<<!TYPE_VARIANCE_CONFLICT_ERROR!>S<!>>
