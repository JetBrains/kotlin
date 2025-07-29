interface Container<A>
interface TContainer<B> : Container<B>

abstract class TNumberBoundContainer<C : Number> : Container<C>

class NumberContainer : Container<Number>
object IntContainer : Container<Int>
object StringContainer : Container<String>

// type parameter is fixed to the final class
fun Container<Int>.containerIntExtension() {}

/**
 * [Container.containe<caret_1>rIntExtension] - resolved
 * [TContainer.containerIntExte<caret_2>nsion] - resolved
 * [TNumberBoundContainer.containerI<caret_3>ntExtension] - resolved
 * [NumberContainer.contain<caret_4>erIntExtension] - UNRESOLVED
 * [IntContainer.containerInt<caret_5>Extension] - resolved
 * [StringContainer.containe<caret_6>rIntExtension] - UNRESOLVED
 */
fun testContainerIntExtension() {}

// type parameter is fixed to an abstract class
fun Container<Number>.containerFixedNumberExtension() {}

/**
 * [Container.containerFixedN<caret_7>umberExtension] - resolved
 * [TContainer.containerFixed<caret_8>NumberExtension] - resolved
 * [TNumberBoundContainer.contai<caret_9>nerFixedNumberExtension] - resolved
 * [NumberContainer.containerFixe<caret_10>dNumberExtension] - resolved
 * [IntContainer.containerFixed<caret_11>NumberExtension] - UNRESOLVED
 * [StringContainer.containerFixe<caret_12>dNumberExtension] - UNRESOLVED
 */
fun testContainerFixedNumberExtension() {}

// type parameter is out projection of an abstract class
fun Container<out Number>.containerOutNumberExtension() {}

/**
 * [Container.containerOutNu<caret_13>mberExtension] - resolved
 * [TContainer.containerOutNumbe<caret_14>rExtension] - resolved
 * [TNumberBoundContainer.containerOutN<caret_15>umberExtension] - resolved
 * [NumberContainer.container<caret_16>OutNumberExtension] - resolved
 * [IntContainer.containerOutNumberEx<caret_17>tension] - resolved
 * [StringContainer.containerOutNumberExte<caret_18>nsion] - UNRESOLVED
 */
fun testContainerOutNumberExtension() {}