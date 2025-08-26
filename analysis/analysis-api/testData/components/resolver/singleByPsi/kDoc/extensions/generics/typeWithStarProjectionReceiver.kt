interface Container<A>
interface TContainer<B> : Container<B>

abstract class TNumberBoundContainer<C : Number> : Container<C>

class NumberContainer : Container<Number>
object IntContainer : Container<Int>
object StringContainer : Container<String>


fun Container<*>.containerExtension() {}

/**
 * [Container.container<caret_1>Extension] - resolved
 * [TContainer.container<caret_2>Extension] - resolved
 * [TNumberBoundContainer.contain<caret_3>erExtension] - resolved
 * [NumberContainer.contain<caret_4>erExtension] - resolved
 * [IntContainer.containe<caret_5>rExtension] - resolved
 * [StringContainer.container<caret_6>Extension] - resolved
 */
fun testContainerExtension() {}