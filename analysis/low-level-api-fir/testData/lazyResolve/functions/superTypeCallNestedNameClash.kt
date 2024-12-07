// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package second

open class Base<T>

class MyCla<caret>ss() : Base<Base<Int>>() {
    open class Base<T>
}
