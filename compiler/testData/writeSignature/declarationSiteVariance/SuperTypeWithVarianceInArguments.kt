
interface Base<A, in B, out C>
interface Intermediate<A>
class Derived<A, B, C>: Intermediate<Base<A, B, C>>

// class: Derived
// jvm signature:     Derived
// generic signature: <A:Ljava/lang/Object;B:Ljava/lang/Object;C:Ljava/lang/Object;>Ljava/lang/Object;LIntermediate<LBase<TA;-TB;+TC;>;>;
