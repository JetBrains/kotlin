// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
package second

interface Base<A> {
    fun foo() {}
}

class MyCla<caret>ss(val prop: second.Base<second.Base<Int>>): Base<Base<Int>> by prop {
    interface Base<B>
}
