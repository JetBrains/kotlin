class Out<out T>

class Final
open class Open

fun deepOpen(x: Out<Out<Out<Open>>>) {}
// method: DeepOutKt::deepOpen
// generic signature: (LOut<+LOut<+LOut<+LOpen;>;>;>;)V

fun deepFinal(x: Out<Out<Out<Final>>>) {}
// method: DeepOutKt::deepFinal
// generic signature: (LOut<LOut<LOut<LFinal;>;>;>;)V
