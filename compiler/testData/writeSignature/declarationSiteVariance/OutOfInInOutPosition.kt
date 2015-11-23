class In<in T>
class Out<out T>
class X

fun f(): Out<In<X>> = throw Exception()

// method: OutOfInInOutPositionKt::f
// jvm signature:     ()LOut;
// generic signature: ()LOut<LIn<LX;>;>;