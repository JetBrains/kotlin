class Out<out T>
class X

// Why we want this to be translated to 'Out<Out<X>> f()' instead of 'Out<? extends Out<? extebds X>> f()'
// For return types default behaviour is skipping all declaration-site wildcards.
// The intuition behind this rule is simple: return types are basically used in subtype position
// (as an argument for another call), and here everything works well in case of 'out'-variance.
// For example we have 'Out<Out<T>>>' as subtype both for 'Out<Out<T>>>' and 'Out<? extends Out<? extends T>>>',
// so values of such type is more flexible in contrast to `Out<? extends Out<? extends T>>>` that could be used only
// for the second case.
fun f(): Out<Out<X>> = throw Exception()

// method: OutOfOutInOutPositionKt::f
// jvm signature:     ()LOut;
// generic signature: ()LOut<LOut<LX;>;>;