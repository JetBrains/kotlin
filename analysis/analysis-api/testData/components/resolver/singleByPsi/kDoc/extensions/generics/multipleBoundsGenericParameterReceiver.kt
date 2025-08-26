open class A
interface B
interface C

interface Container<T>

fun <T : C> Container<T>.someExtension() {}


class BoundedContainer<T> : Container<T> where T : A, T : B

/**
 * [BoundedContainer.someEx<caret>tension] - resolved
 */
fun foo() {}