class M<in K, out V>
class X

val p: M<X, X> = throw Exception()

// method: PropertyGetterTwoParamsKt::getP
// jvm signature:     ()LM;
// generic signature: ()LM<LX;LX;>;