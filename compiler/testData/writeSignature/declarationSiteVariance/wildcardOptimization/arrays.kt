class Inv<E>
class Out<out T>
class OutPair<out Final, out Y>
class In<in Z>

class Final
open class Open

fun arrayOfOutOpen(x: Array<Out<Open>>) {}
// method: ArraysKt::arrayOfOutOpen
// generic signature: ([LOut<LOpen;>;)V

fun arrayOfOutFinal(x: Array<Out<Final>>) {}
// method: ArraysKt::arrayOfOutFinal
// generic signature: ([LOut<LFinal;>;)V

fun outOfArrayOpen(x: Out<Array<Open>>) {}
// method: ArraysKt::outOfArrayOpen
// generic signature: (LOut<[LOpen;>;)V

fun outOfArrayOutOpen(x: Out<Array<out Open>>) {}
// method: ArraysKt::outOfArrayOutOpen
// generic signature: (LOut<+[LOpen;>;)V
