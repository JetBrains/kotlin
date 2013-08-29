class Out<out T>
class X

fun f(p: Out<Out<X>>) {}

// method: _DefaultPackage::f
// jvm signature:     (LOut;)V
// generic signature: (LOut<+LOut<+LX;>;>;)V