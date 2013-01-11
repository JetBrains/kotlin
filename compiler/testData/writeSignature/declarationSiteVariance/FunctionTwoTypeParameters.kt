class M<in K, out V>

class X

fun f(m: M<X, X>): M<X, X> = throw Exception()

// method: _DefaultPackage::f
// jvm signature:     (LM;)LM;
// generic signature: (LM<-LX;+LX;>;)LM<LX;LX;>;
// kotlin signature:  (LM<LX;LX;>;)LM<LX;LX;>;
