// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package second

abstract class S<caret>ubClass: AbstractClass<Int>(0)

abstract class AbstractClass<T> {
    constructor(t: T)
}
