class In<in T>
class Out<out T>
class X

fun f(): Out<In<X>> = throw Exception()

// method: namespace::f
// jvm signature:     ()LOut;
// generic signature: ()LOut<LIn<-LX;>;>;
// kotlin signature:  ()LOut<LIn<LX;>;>;
