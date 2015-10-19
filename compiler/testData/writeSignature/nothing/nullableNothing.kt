class C<T>
fun f(p: Nothing?, p1: C<Nothing?>, p2: C<C<Nothing?>>, p3: C<C<Nothing?>>?): Nothing? = throw Exception()

// method: _DefaultPackage::f
// jvm signature: (Ljava/lang/Void;LC;LC;LC;)Ljava/lang/Void;
// generic signature: null
