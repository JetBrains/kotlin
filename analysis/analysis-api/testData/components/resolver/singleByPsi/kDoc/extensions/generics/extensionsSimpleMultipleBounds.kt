interface A
interface B
interface C : A, B

interface GenericFooOut<out T>

interface GenericWithBound<T>: GenericFooOut<T> where T: B

fun <T> GenericFooOut<T>.ext() where T: A{}

class SomeClass: GenericWithBound<C> {}

/**
 * [SomeClass.ex<caret_1>t]
 */
fun usage() {
    SomeClass().ext()
}

