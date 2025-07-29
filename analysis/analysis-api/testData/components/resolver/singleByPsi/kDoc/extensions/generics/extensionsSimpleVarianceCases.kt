interface A
interface B : A

interface GenericFooOut<out T>

fun GenericFooOut<A>.ext() {}

class SomeClassOut: GenericFooOut<B> {}

interface GenericFooIn<in T>

fun GenericFooIn<B>.ext() {}

class SomeClassIn: GenericFooIn<A> {}

interface GenericFoo<T>

fun GenericFoo<A>.ext() {}

class SomeClass: GenericFoo<A> {}

/**
 * [SomeClass.ex<caret_1>t]
 * [SomeClassIn.ex<caret_2>t]
 * [SomeClassOut.ex<caret_3>t]
 */
fun usage() {}

