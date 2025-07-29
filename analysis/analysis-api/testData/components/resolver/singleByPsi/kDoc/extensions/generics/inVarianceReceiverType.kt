interface OutContainer<in T>

class NumberOutContainer : OutContainer<Number>

class IntOutContainer : OutContainer<Int>

fun OutContainer<Number>.someNumberExtension() {}
fun OutContainer<Int>.someIntExtension() {}

/**
 * [OutContainer.someNumber<caret_1>Extension] - resolved
 * [IntOutContainer.someNumberE<caret_2>xtension] - UNRESOLVED
 * [NumberOutContainer.someNumb<caret_3>erExtension] - resolved
 * [OutContainer.someIntEx<caret_4>tension] - resolved
 * [IntOutContainer.someInt<caret_5>Extension] - resolved
 * [NumberOutContainer.someInt<caret_6>Extension] - resolved
 */
fun foo() {}