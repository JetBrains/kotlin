class Out<out T>
class OutPair<out X, out Y>
class In<in Z>

class Final
open class Open

// For value parameters we decided to skip wildcards if it doesn't make obtained signature weaker
// in a sense of set of acceptable arguments.
// More precisely:
//    a. We write wildcard for 'Out<T>' iff T ``can have subtypes ignoring nullability''
//    b. We write wildcard for 'In<T>' iff T is not equal to it's class upper bound (ignoring nullability again)
// Definition of ``can have subtypes ignoring nullability'' is straightforward and you can see it in commit.

fun openClassArgument(x: Out<Open>, y: In<Open>) {}
// method: ArgumentOverridabilityKt::openClassArgument
// generic signature: (LOut<+LOpen;>;LIn<-LOpen;>;)V

fun finalClassArgument(x: Out<Final>, y: In<Final>) {}
// method: ArgumentOverridabilityKt::finalClassArgument
// generic signature: (LOut<LFinal;>;LIn<-LFinal;>;)V

fun oneArgumentFinal(x: OutPair<Final, Open>) {}
// method: ArgumentOverridabilityKt::oneArgumentFinal
// generic signature: (LOutPair<LFinal;+LOpen;>;)V
