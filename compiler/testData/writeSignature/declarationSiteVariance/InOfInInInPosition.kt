class In<in T>
class X

fun f(p: In<In<X>>) {}

// method: InOfInInInPositionKt::f
// jvm signature:     (LIn;)V
// generic signature: (LIn<-LIn<-LX;>;>;)V