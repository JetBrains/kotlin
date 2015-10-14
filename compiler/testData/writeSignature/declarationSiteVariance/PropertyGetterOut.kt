class M<out V>
class X

val p: M<X> = throw Exception()

// method: PropertyGetterOutKt::getP
// jvm signature:     ()LM;
// generic signature: ()LM<LX;>;