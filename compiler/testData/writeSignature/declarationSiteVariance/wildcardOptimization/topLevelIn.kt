class In<in Z>
class Out<out T>
class Final

fun inFinal(x: In<Final>) {}
// method: TopLevelInKt::inFinal
// generic signature: (LIn<-LFinal;>;)V

fun inAny(x: In<Any>) {}
// method: TopLevelInKt::inAny
// generic signature: (LIn<Ljava/lang/Object;>;)V

fun inOutFinal(x: In<Out<Final>>) {}
// method: TopLevelInKt::inOutFinal
// generic signature: (LIn<-LOut<LFinal;>;>;)V
