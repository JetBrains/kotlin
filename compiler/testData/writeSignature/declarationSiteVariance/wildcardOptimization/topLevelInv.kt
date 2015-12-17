class Inv<X>
class In<in E>
class Out<out T>
class Final
open class Open

fun invOpen(x: Inv<Open>) {}
// method: TopLevelInvKt::invOpen
// generic signature: (LInv<LOpen;>;)V

fun invFinal(x: Inv<Final>) {}
// method: TopLevelInvKt::invFinal
// generic signature: (LInv<LFinal;>;)V

fun invOutOpen(x: Inv<Out<Open>>) {}
// method: TopLevelInvKt::invOutOpen
// generic signature: (LInv<LOut<LOpen;>;>;)V

fun invOutFinal(x: Inv<Out<Final>>) {}
// method: TopLevelInvKt::invOutFinal
// generic signature: (LInv<LOut<LFinal;>;>;)V

fun invInOutOpen(x: Inv<In<Out<Open>>>) {}
// method: TopLevelInvKt::invInOutOpen
// generic signature: (LInv<LIn<LOut<+LOpen;>;>;>;)V

fun invInOutFinal(x: Inv<In<Out<Final>>>) {}
// method: TopLevelInvKt::invInOutFinal
// generic signature: (LInv<LIn<LOut<LFinal;>;>;>;)V

fun invOutProjectedOutFinal(x: Inv<out Out<Final>>) {}
// method: TopLevelInvKt::invOutProjectedOutFinal
// generic signature: (LInv<+LOut<LFinal;>;>;)V
