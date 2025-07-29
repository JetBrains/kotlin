interface GenericFooOut<out T>

fun <T: GenericFooOut<T>> GenericFooOut<T>.ext() {}

class SomeClassOut: GenericFooOut<SomeClassOut> {}

interface GenericFooIn<in T>

fun <T: GenericFooIn<T>> GenericFooIn<T>.ext() {}

class SomeClassIn: GenericFooIn<SomeClassIn> {}

interface GenericFoo<T>

fun <T: GenericFoo<T>> GenericFoo<T>.ext() {}

class SomeClass: GenericFoo<SomeClass> {}

/**
 * [SomeClass.ex<caret_1>t]
 * [SomeClassIn.ex<caret_2>t]
 * [SomeClassOut.ex<caret_3>t]
 */
fun usage() {}
