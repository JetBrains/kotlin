// !DIAGNOSTICS: -UNUSED_TYPEALIAS_PARAMETER

interface I1<S>
interface Out<out T>
interface InvOut<T1, out T2>

typealias A1<S> = I1<S>
typealias A2<T, S> = I1<S>
typealias AOut<T> = Out<T>
typealias AInvOut<T1, T2> = InvOut<T1, T2>
typealias AInvOutTT<T> = AInvOut<T, T>

class Test1<out S> : <!TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE!>A1<S><!>
class Test2<out S> : <!TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE!>A2<Any, S><!>
class Test3<out S> : AOut<S>
class Test4<out S> : <!TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE!>AInvOut<S, S><!>
class Test5<out S> : <!TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE!>AInvOutTT<S><!>