// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package second

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

open class Base<T>

const val outer = 0
const val inner = ""

class MyCla<caret>ss() : @Anno(1 + outer) Base<@Anno(2 + outer) Base<@Anno(3 + outer) Int>>() {
    open class Base<T>

    companion object {
        const val outer = ""
        const val inner = 0
    }
}
