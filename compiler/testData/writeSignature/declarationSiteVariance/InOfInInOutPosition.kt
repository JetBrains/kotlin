class In<in T>
class X

fun f(): In<In<X>> = throw Exception()

// method: _DefaultPackage::f
// jvm signature:     ()LIn;
// generic signature: ()LIn<LIn<-LX;>;>;