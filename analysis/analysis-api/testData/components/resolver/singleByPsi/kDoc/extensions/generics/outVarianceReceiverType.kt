interface OutContainer<out T>

class NumberOutContainer : OutContainer<Number>

class IntOutContainer : OutContainer<Int>

fun OutContainer<Number>.someNumberExtension() {}
fun OutContainer<Int>.someIntExtension() {}

/**
 * [OutContainer.someNumberE<caret_1>xtension] - resolved
 * [IntOutContainer.someNumber<caret_2>Extension] - resolved
 * [NumberOutContainer.someNu<caret_3>mberExtension] - resolved
 * [OutContainer.someInt<caret_4>Extension] - resolved
 * [IntOutContainer.someInt<caret_5>Extension] - resolved
 * [NumberOutContainer.someIntEx<caret_6>tension] - UNRESOLVED
 */
fun foo() {}