class Inv<E>
class Out<out T>

class Final
open class Open

fun invInv(x: Out<Inv<Open>>) {}
// method: OutInvKt::invInv
// generic signature: (LOut<LInv<LOpen;>;>;)V

fun invOut(x: Out<Inv<out Open>>) {}
// method: OutInvKt::invOut
// generic signature: (LOut<+LInv<+LOpen;>;>;)V

fun invOutFinal(x: Out<Inv<out Final>>) {}
// method: OutInvKt::invOutFinal
// generic signature: (LOut<LInv<+LFinal;>;>;)V

fun invIn(x: Out<Inv<in Final>>) {}
// method: OutInvKt::invIn
// generic signature: (LOut<+LInv<-LFinal;>;>;)V

fun invInAny(x: Out<Inv<in Any>>) {}
// method: OutInvKt::invInAny
// generic signature: (LOut<LInv<-Ljava/lang/Object;>;>;)V
