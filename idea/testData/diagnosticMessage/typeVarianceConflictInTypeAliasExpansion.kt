// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE
// !MESSAGE_TYPE: TEXT

interface InvOut<T1, out T2>

typealias AInvOut<T1, T2> = InvOut<T1, T2>
typealias AInvOutTT<T> = AInvOut<T, T>

class Test<out S> : AInvOutTT<S>