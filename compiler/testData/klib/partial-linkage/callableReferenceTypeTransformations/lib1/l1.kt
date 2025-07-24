interface XAnswer { fun answer(): Int }
interface XAnswerDefault { fun answer(): Int /*= 42*/ }
interface XFunction1 { /*fun function1(): Int*/ }
interface XFunction1Default { /*fun function1(): Int = 42*/ }
interface XFunction2 { /*fun function2(): Int*/ }
interface XFunction2Default { /*fun function2(): Int = -42*/ }
interface XProperty1 { /*val property1: Int*/ }
interface XProperty1Default { /*val property1: Int get() = 42*/ }
interface XProperty2 { /*val property2: Int*/ }
interface XProperty2Default { /*val property2: Int get() = 42*/ }

fun interface FunctionalInterfaceToInterface : XAnswer

fun interface FunInterfaceWithChangedFun {
    fun answer(/*x: Int*/): Int
}

fun interface FunInterfaceWithDifferentAbstractFun {
    fun answer(): Int /* = 42 */
    /*fun hijack(): Int*/
}

fun interface FunInterfaceWithDifferentChangedAbstractFun {
    fun answer(): Int /* = 42 */
    /*fun hijack(x: Int): Int*/
}
