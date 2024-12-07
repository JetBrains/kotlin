// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package second

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

open class Base

const val outer = 0
const val inner = ""

class MyC<caret>lass() : @Anno(1 + outer) Base() {
    open class Base

    companion object {
        const val outer = ""
        const val inner = 0
    }
}
