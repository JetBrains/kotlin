class In<in T>
class Out<out T>
class X

fun f(): In<Out<X>> = throw Exception()

// method: _DefaultPackage::f
// jvm signature:     ()LIn;
// generic signature: ()LIn<LOut<+LX;>;>;