class In<in T>
class Out<out T>
class X

fun f(p: Out<In<X>>) {}

// method: _DefaultPackage::f
// jvm signature:     (LOut;)V
// generic signature: (LOut<+LIn<-LX;>;>;)V
// kotlin signature:  (LOut<LIn<LX;>;>;)V
