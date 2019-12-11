interface In<in T>
interface Out<out T>
interface Inv<T>

interface TypeBounds1<in I, out O, P, X : I>
interface TypeBounds2<in I, out O, P, X : O>
interface TypeBounds3<in I, out O, P, X : P>
interface TypeBounds4<in I, out O, P, X : In<I>>
interface TypeBounds5<in I, out O, P, X : In<O>>

interface WhereTypeBounds1<in I, out O, P, X> where X : I
interface WhereTypeBounds2<in I, out O, P, X> where X : O
interface WhereTypeBounds3<in I, out O, P, X> where X : P
interface WhereTypeBounds4<in I, out O, P, X> where X : In<I>
interface WhereTypeBounds5<in I, out O, P, X> where X : In<O>

class SubClass1<in I, out O, P> : Out<I>
class SubClass2<in I, out O, P> : Out<O>
class SubClass3<in I, out O, P> : Out<P>
class SubClass4<in I, out O, P> : In<I>
class SubClass5<in I, out O, P> : In<O>
class SubClass6<in I, out O, P> : Inv<O>
class SubClass7<in I, out O, P> : Inv<I>