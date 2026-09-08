// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// SAM_CONVERSIONS: INDY
// LAMBDAS: CLASS

// CHECK_BYTECODE_TEXT
// 0 java/lang/invoke/LambdaMetafactory

// The SAM method parameter is a type parameter with two interface upper bounds, so it is erased to the
// first one ('A'), while the lambda parameter is 'B'. LambdaMetafactory can't adapt an instantiated
// method type '(LA;)V' to an implementation method '(LB;)V', so this SAM conversion must stay
// class-based instead of failing with a LambdaConversionException on JDK 9+.

interface A
interface B

class AB : A, B

fun interface Sink<T> {
    fun accept(value: T)
}

var result = "Fail"

fun <T> makeSink(): Sink<T> where T : A, T : B =
    Sink { _: B -> result = "OK" }

fun box(): String {
    makeSink<AB>().accept(AB())
    return result
}
