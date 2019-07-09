interface In<in T>
interface Out<out T>
interface Inv<T>

interface TypeBounds1<in I, out O, P, X : <!TYPE_VARIANCE_CONFLICT("I", "in", "out", "I")!>I<!>>
interface TypeBounds2<in I, out O, P, X : O>
interface TypeBounds3<in I, out O, P, X : P>
interface TypeBounds4<in I, out O, P, X : In<I>>
interface TypeBounds5<in I, out O, P, X : In<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<O>")!>O<!>>>

interface WhereTypeBounds1<in I, out O, P, X> where X : <!TYPE_VARIANCE_CONFLICT("I", "in", "out", "I")!>I<!>
interface WhereTypeBounds2<in I, out O, P, X> where X : O
interface WhereTypeBounds3<in I, out O, P, X> where X : P
interface WhereTypeBounds4<in I, out O, P, X> where X : In<I>
interface WhereTypeBounds5<in I, out O, P, X> where X : In<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<O>")!>O<!>>

class SubClass1<in I, out O, P> : Out<<!TYPE_VARIANCE_CONFLICT("I", "in", "out", "Out<I>")!>I<!>>
class SubClass2<in I, out O, P> : Out<O>
class SubClass3<in I, out O, P> : Out<P>
class SubClass4<in I, out O, P> : In<I>
class SubClass5<in I, out O, P> : In<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<O>")!>O<!>>
class SubClass6<in I, out O, P> : Inv<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Inv<O>")!>O<!>>
class SubClass7<in I, out O, P> : Inv<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Inv<I>")!>I<!>>