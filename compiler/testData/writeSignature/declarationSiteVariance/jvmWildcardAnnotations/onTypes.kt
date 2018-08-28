// WITH_RUNTIME
class OutPair<out T, out E>
class Out<out F>
class In<in H>

class X

fun simpleOut(x: Out<@JvmWildcard X>) {}
// method: OnTypesKt::simpleOut
// generic signature: (LOut<+LX;>;)V

fun simpleIn(x: In<@JvmWildcard Any?>) {}
// method: OnTypesKt::simpleIn
// generic signature: (LIn<-Ljava/lang/Object;>;)V

fun falseTrueFalse(): @JvmSuppressWildcards(false) OutPair<X, @JvmSuppressWildcards OutPair<Out<X>, Out<@JvmSuppressWildcards(false) X>>> = null!!
// method: OnTypesKt::falseTrueFalse
// generic signature: ()LOutPair<+LX;LOutPair<LOut<LX;>;LOut<+LX;>;>;>;

open class Open
fun combination(): @JvmSuppressWildcards OutPair< Open, @JvmWildcard OutPair<Open, @JvmWildcard Out<Open>>> = null!!
// method: OnTypesKt::combination
// generic signature: ()LOutPair<LOpen;+LOutPair<LOpen;+LOut<LOpen;>;>;>;
