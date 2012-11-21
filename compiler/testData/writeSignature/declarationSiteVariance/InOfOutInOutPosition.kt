class In<in T>
class Out<out T>
class X

fun f(): In<Out<X>> = throw Exception()

// method: namespace::f
// jvm signature:     ()LIn;
// generic signature: ()LIn<LOut<+LX;>;>;
// kotlin signature:  ()LIn<LOut<LX;>;>;
