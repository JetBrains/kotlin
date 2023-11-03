// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
package second

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

interface Base {
    fun foo() {}
}

const val outer = 0
const val inner = ""
class My<caret>Class(val prop: @Anno(0 + inner) second.Base): @Anno(1 + outer) Base by prop {
    interface Base

    companion object {
        const val outer = ""
        const val inner = 0
    }
}
