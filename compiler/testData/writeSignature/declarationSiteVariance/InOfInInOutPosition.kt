class In<in T>
class X

fun f(): In<In<X>> = throw Exception()

// method: namespace::f
// jvm signature:     ()LIn;
// generic signature: ()LIn<LIn<-LX;>;>;
// kotlin signature:  ()LIn<LIn<LX;>;>;
