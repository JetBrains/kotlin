// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
package second

interface Base {
    fun foo() {}
}

class My<caret>Class(val prop: second.Base): Base by prop {
    interface Base
}
